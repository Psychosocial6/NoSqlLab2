package com.example.nosqllab2.analytics;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.example.nosqllab2.storage.FirestoreCalls.await;

@Repository
@RequiredArgsConstructor
public class ProductChangeRepository {
    private static final int SHARDS = 16;

    private final Firestore firestore;

    public void recordUpdate(Transaction transaction, String productId) {
        String interval = ChangeIntervals.currentKey();
        int shard = Math.floorMod(productId.hashCode(), SHARDS);
        transaction.set(stats().document(interval + "_" + shard), Map.of(
                "intervalKey", interval,
                "changeCount", FieldValue.increment(1),
                "lastUpdated", FieldValue.serverTimestamp()), SetOptions.merge());
    }

    List<IntervalShard> read(ChangeIntervals.Range range) {
        return await(stats().whereGreaterThanOrEqualTo("intervalKey", range.from())
                .whereLessThanOrEqualTo("intervalKey", range.to())
                .orderBy("intervalKey").get()).getDocuments().stream()
                .map(ProductChangeRepository::fromDocument)
                .toList();
    }

    private static IntervalShard fromDocument(QueryDocumentSnapshot document) {
        var lastUpdated = document.getDate("lastUpdated");
        Long changeCount = document.getLong("changeCount");
        return new IntervalShard(document.getString("intervalKey"), changeCount == null ? 0 : changeCount,
                lastUpdated == null ? Instant.EPOCH : lastUpdated.toInstant());
    }

    private CollectionReference stats() {
        return firestore.collection("product_change_stats");
    }

    record IntervalShard(String interval, long count, Instant lastUpdated) {
        IntervalShard combine(IntervalShard other) {
            Instant latest = lastUpdated.isAfter(other.lastUpdated) ? lastUpdated : other.lastUpdated;
            return new IntervalShard(interval, count + other.count, latest);
        }
    }
}
