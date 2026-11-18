package com.example.nosqllab2.migration;

import com.example.nosqllab2.products.Product;
import com.example.nosqllab2.products.ProductRepository;
import com.example.nosqllab2.products.ProductStatus;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldMask;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.example.nosqllab2.storage.FirestoreCalls.await;

@RequiredArgsConstructor
@Service
public class ProductMigrationService {
    private static final int BATCH_SIZE = 100;
    private static final String[] FIELDS = {"status", "updatedStatus", "categoryId"};

    private final Firestore firestore;
    private final ProductRepository products;

    public MigrationResult migrateProductStatus() {
        Query page = firestore.collection("products").select(FIELDS).orderBy(FieldPath.documentId()).limit(BATCH_SIZE);
        int migrated = 0;
        int skipped = 0;
        for (List<QueryDocumentSnapshot> documents = await(page.get()).getDocuments();
             !documents.isEmpty();
             documents = await(page.startAfter(documents.getLast()).get()).getDocuments()) {
            int updated = migratePage(documents);
            migrated += updated;
            skipped += documents.size() - updated;
            if (documents.size() < BATCH_SIZE) {
                break;
            }
        }
        return new MigrationResult("SUCCESS", migrated, skipped);
    }

    private int migratePage(List<QueryDocumentSnapshot> documents) {
        DocumentReference[] references = documents.stream()
                .map(DocumentSnapshot::getReference).toArray(DocumentReference[]::new);
        return await(firestore.runTransaction(transaction -> {
            List<DocumentSnapshot> snapshots = await(transaction.getAll(references, FieldMask.of(FIELDS)));
            Set<String> categories = new HashSet<>();
            int updated = 0;
            for (DocumentSnapshot snapshot : snapshots) {
                Product product = snapshot.toObject(Product.class);
                if (product == null) {
                    continue;
                }
                Map<String, Object> changes = statusChanges(product);
                if (!changes.isEmpty()) {
                    transaction.update(snapshot.getReference(), changes);
                    categories.add(product.getCategoryId());
                    updated++;
                }
            }
            if (updated > 0) {
                products.advanceRevisions(transaction, categories.toArray(String[]::new));
            }
            return updated;
        }));
    }

    static Map<String, Object> statusChanges(Product product) {
        Map<String, Object> changes = new HashMap<>();
        String code = product.getActualStatus().getCode();
        if (!Objects.equals(code, product.getStatus())) {
            changes.put("status", code);
        }
        ProductStatus current = product.getUpdatedStatus();
        if (current == null || !Product.hasStatusCode(current.getCode())) {
            changes.put("updatedStatus", new ProductStatus(code, new Date(), "updatedStatus"));
        }
        return changes;
    }

    public record MigrationResult(String status, int migratedCount, int skippedCount) {
    }
}
