package com.example.nosqllab2.clientLeads;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Map;

import static com.example.nosqllab2.storage.FirestoreCalls.await;

@Repository
@RequiredArgsConstructor
public class LeadRepository {
    private final Firestore firestore;

    public long create(String userId) {
        DocumentReference sequence = firestore.collection("sequences").document("lead");
        return await(firestore.runTransaction(transaction -> {
            Long previous = await(transaction.get(sequence)).getLong("lastId");
            long id = previous == null ? 1 : Math.incrementExact(previous);
            transaction.set(sequence, Map.of("lastId", id));
            transaction.create(firestore.collection("leads").document(Long.toString(id)), Map.of(
                    "userId", userId, "createdAt", FieldValue.serverTimestamp()));
            return id;
        }));
    }
}
