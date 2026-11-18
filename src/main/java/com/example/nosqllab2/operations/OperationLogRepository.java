package com.example.nosqllab2.operations;

import com.example.nosqllab2.exceptions.ApplicationException;
import com.example.nosqllab2.storage.DocumentPage;
import com.example.nosqllab2.storage.FirestoreRevisions;
import com.example.nosqllab2.storage.StorageKey;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.Map;

import static com.example.nosqllab2.storage.FirestoreCalls.await;

@Repository
@RequiredArgsConstructor
public class OperationLogRepository {
    private final Firestore firestore;
    private final FirestoreRevisions revisions;

    public void append(Transaction transaction, String userId, String operation) {
        transaction.create(logs().document(), Map.of(
                "userId", userId, "operation", operation, "operationTime", FieldValue.serverTimestamp()));
        revisions.advance(transaction, scope(userId));
    }

    public DocumentPage<OperationLogDTO> list(String userId, int size, String afterId) {
        Query query = logs().whereEqualTo("userId", userId)
                .orderBy("operationTime", Query.Direction.DESCENDING)
                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING);
        if (afterId != null) {
            DocumentSnapshot cursor = await(logs().document(afterId).get());
            if (!cursor.exists() || !userId.equals(cursor.getString("userId")) || cursor.getDate("operationTime") == null) {
                throw new ApplicationException(HttpStatus.BAD_REQUEST, "Pagination cursor is no longer valid");
            }
            query = query.startAfter(cursor);
        }
        return DocumentPage.from(await(query.limit(size + 1).get()).getDocuments(), size,
                document -> OperationLogDTO.fromEntity(document.toObject(OperationLog.class)));
    }

    public long revision(String userId) {
        return revisions.read(scope(userId));
    }

    private CollectionReference logs() {
        return firestore.collection("operation_logs");
    }

    private static String scope(String userId) {
        return "operations-" + StorageKey.hash(userId);
    }
}
