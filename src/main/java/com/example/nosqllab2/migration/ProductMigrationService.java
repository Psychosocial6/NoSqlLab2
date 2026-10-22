package com.example.nosqllab2.migration;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductMigrationService {

    private final Firestore firestore;

    public Map<String, Object> migrateProductStatus() {
        try {
            List<QueryDocumentSnapshot> documents = firestore.collection("products").get().get().getDocuments();

            int migratedCount = 0;
            int skippedCount = 0;
            WriteBatch batch = firestore.batch();

            for (DocumentSnapshot doc : documents) {
                if (doc.contains("updatedStatus") && doc.get("updatedStatus") != null) {
                    skippedCount++;
                    continue;
                }

                String oldStatusString = doc.getString("status");
                if (oldStatusString == null || oldStatusString.isBlank()) {
                    oldStatusString = "AVAILABLE";
                }

                Map<String, Object> newStatusObject = new HashMap<>();
                newStatusObject.put("code", oldStatusString);
                newStatusObject.put("updatedAt", new Date());
                newStatusObject.put("comment", "updatedStatus");

                DocumentReference ref = doc.getReference();

                batch.update(ref, "updatedStatus", newStatusObject);
                migratedCount++;
            }

            if (migratedCount > 0) {
                batch.commit().get();
            }

            log.info("Migration finished: migrated={}, skipped={}", migratedCount, skippedCount);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "SUCCESS");
            response.put("migratedCount", migratedCount);
            response.put("skippedCount", skippedCount);
            return response;

        } catch (Exception e) {
            throw new RuntimeException("Migration failed: " + e.getMessage(), e);
        }
    }
}