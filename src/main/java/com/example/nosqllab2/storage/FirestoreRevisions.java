package com.example.nosqllab2.storage;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Map;

import static com.example.nosqllab2.storage.FirestoreCalls.await;

@Repository
@RequiredArgsConstructor
public class FirestoreRevisions {
    private final Firestore firestore;

    public long read(String scope) {
        Long revision = await(reference(scope).get()).getLong("revision");
        return revision == null ? 0 : revision;
    }

    public void advance(Transaction transaction, String scope) {
        transaction.set(reference(scope), Map.of("revision", FieldValue.increment(1)), SetOptions.merge());
    }

    private DocumentReference reference(String scope) {
        return firestore.collection("cache_revisions").document(scope);
    }
}
