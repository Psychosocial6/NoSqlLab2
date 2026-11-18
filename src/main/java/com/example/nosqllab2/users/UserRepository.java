package com.example.nosqllab2.users;

import com.example.nosqllab2.exceptions.ApplicationException;
import com.example.nosqllab2.exceptions.UserAlreadyExistsException;
import com.example.nosqllab2.exceptions.UserNotFoundException;
import com.example.nosqllab2.storage.DocumentPage;
import com.example.nosqllab2.storage.StorageKey;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldMask;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.SetOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.example.nosqllab2.storage.FirestoreCalls.await;

@Repository
@RequiredArgsConstructor
public class UserRepository {
    private static final int INDEX_PAGE_SIZE = 100;

    private final Firestore firestore;
    private volatile boolean usernameIndexReady;

    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        ensureUsernameIndex();
        List<String> ids = userIds(await(usernameReference(username).get()));
        if (ids.size() == 1) {
            return findById(ids.getFirst());
        }
        if (ids.isEmpty()) {
            return Optional.empty();
        }
        // Preserve access to old accounts whose names differ only by case.
        DocumentReference[] references = ids.stream().map(id -> users().document(id)).toArray(DocumentReference[]::new);
        List<User> exactMatches = await(firestore.getAll(references)).stream()
                .filter(DocumentSnapshot::exists)
                .filter(document -> username.strip().equals(document.getString("name")))
                .map(document -> document.toObject(User.class))
                .limit(2)
                .toList();
        return exactMatches.size() == 1 ? Optional.of(exactMatches.getFirst()) : Optional.empty();
    }

    public Optional<User> findById(String id) {
        return Optional.ofNullable(await(users().document(id).get()).toObject(User.class));
    }

    public User create(User user) {
        ensureUsernameIndex();
        DocumentReference name = usernameReference(user.getName());
        await(firestore.runTransaction(transaction -> {
            if (await(transaction.get(name)).exists()) {
                throw new UserAlreadyExistsException("User already exists");
            }
            transaction.create(users().document(user.getId()), Map.of(
                    "name", user.getName(), "email", user.getEmail(),
                    "passwordHash", user.getPasswordHash(), "role", user.getRole()));
            transaction.create(name, Map.of("userIds", List.of(user.getId())));
            return null;
        }));
        return user;
    }

    public void delete(String id) {
        ensureUsernameIndex();
        DocumentReference user = users().document(id);
        await(firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = await(transaction.get(user));
            if (!snapshot.exists()) {
                throw new UserNotFoundException("User not found");
            }
            String name = snapshot.getString("name");
            if (name != null && !name.isBlank()) {
                DocumentReference reference = usernameReference(name);
                List<String> ids = new ArrayList<>(userIds(await(transaction.get(reference))));
                if (ids.remove(id)) {
                    if (ids.isEmpty()) {
                        transaction.delete(reference);
                    } else {
                        transaction.update(reference, "userIds", ids);
                    }
                }
            }
            transaction.delete(user);
            return null;
        }));
    }

    public DocumentPage<UserResponse> list(int size, String after) {
        Query query = users().orderBy(FieldPath.documentId());
        if (after != null) {
            query = query.startAfter(after);
        }
        return DocumentPage.from(await(query.limit(size + 1).get()).getDocuments(), size,
                document -> UserResponse.fromEntity(document.toObject(User.class)));
    }

    public boolean upgradePassword(String userId, String expectedCredential, String newHash) {
        DocumentReference reference = users().document(userId);
        return await(firestore.runTransaction(transaction -> {
            User current = await(transaction.get(reference)).toObject(User.class);
            if (current == null || !Objects.equals(PasswordHashes.credential(current), expectedCredential)) {
                return false;
            }
            transaction.update(reference, "passwordHash", newHash, "password", FieldValue.delete());
            return true;
        }));
    }

    private synchronized void initializeUsernameIndex() {
        if (usernameIndexReady) {
            return;
        }
        DocumentReference marker = firestore.collection("system_state").document("username_index_v1");
        if (!await(marker.get()).exists()) {
            backfillUsernameIndex();
            await(marker.set(Map.of("complete", true)));
        }
        usernameIndexReady = true;
    }

    private void ensureUsernameIndex() {
        if (!usernameIndexReady) {
            initializeUsernameIndex();
        }
    }

    private void backfillUsernameIndex() {
        Query page = users().select("name").orderBy(FieldPath.documentId()).limit(INDEX_PAGE_SIZE);
        for (List<QueryDocumentSnapshot> documents = await(page.get()).getDocuments();
             !documents.isEmpty();
             documents = await(page.startAfter(documents.getLast()).get()).getDocuments()) {
            backfillPage(documents);
            if (documents.size() < INDEX_PAGE_SIZE) {
                break;
            }
        }
    }

    private void backfillPage(List<QueryDocumentSnapshot> documents) {
        DocumentReference[] references = documents.stream()
                .map(DocumentSnapshot::getReference).toArray(DocumentReference[]::new);
        await(firestore.runTransaction(transaction -> {
            Map<DocumentReference, List<String>> aliases = new HashMap<>();
            for (DocumentSnapshot document : await(transaction.getAll(references, FieldMask.of("name")))) {
                String name = document.getString("name");
                if (name != null && !name.isBlank()) {
                    aliases.computeIfAbsent(usernameReference(name), ignored -> new ArrayList<>()).add(document.getId());
                }
            }
            aliases.forEach((reference, ids) -> transaction.set(reference,
                    Map.of("userIds", FieldValue.arrayUnion(ids.toArray())), SetOptions.merge()));
            return null;
        }));
    }

    private CollectionReference users() {
        return firestore.collection("users");
    }

    private DocumentReference usernameReference(String name) {
        return firestore.collection("usernames").document(StorageKey.hash(name.strip().toLowerCase(Locale.ROOT)));
    }

    private static List<String> userIds(DocumentSnapshot document) {
        Object value = document.get("userIds");
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> values) {
            return values.stream().map(String.class::cast).toList();
        }
        throw new ApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid username index");
    }
}
