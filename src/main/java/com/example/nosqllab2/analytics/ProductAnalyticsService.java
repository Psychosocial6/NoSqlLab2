package com.example.nosqllab2.analytics;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductAnalyticsService {
    private final Firestore firestore;

    public List<ChangeIntervalStat> getChangeAnalytics(String fromInterval, String toInterval)
            throws ExecutionException, InterruptedException {

        Query query = firestore.collection("product_change_stats")
                .orderBy("intervalKey", Query.Direction.ASCENDING);

        if (fromInterval != null && !fromInterval.isBlank()) {
            query = query.whereGreaterThanOrEqualTo("intervalKey", fromInterval);
        }
        if (toInterval != null && !toInterval.isBlank()) {
            query = query.whereLessThanOrEqualTo("intervalKey", toInterval);
        }

        QuerySnapshot snapshot = query.get().get();

        List<ChangeIntervalStat> result = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
            result.add(new ChangeIntervalStat(
                    doc.getString("intervalKey"),
                    doc.getLong("changeCount"),
                    doc.getDate("lastUpdated") != null ? doc.getDate("lastUpdated").toString() : ""
            ));
        }

        log.info("Returned {} analytical interval stats", result.size());
        return result;
    }
}