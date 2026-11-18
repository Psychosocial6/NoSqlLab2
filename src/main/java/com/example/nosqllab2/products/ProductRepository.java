package com.example.nosqllab2.products;

import com.example.nosqllab2.analytics.ProductChangeRepository;
import com.example.nosqllab2.exceptions.ApplicationException;
import com.example.nosqllab2.exceptions.ProductNotFoundException;
import com.example.nosqllab2.storage.DocumentPage;
import com.example.nosqllab2.storage.FirestoreRevisions;
import com.example.nosqllab2.storage.StorageKey;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.example.nosqllab2.storage.FirestoreCalls.await;

@Repository
@RequiredArgsConstructor
public class ProductRepository {
    private static final String GLOBAL_SCOPE = "products";

    private final Firestore firestore;
    private final FirestoreRevisions revisions;
    private final ProductChangeRepository analytics;

    public DocumentPage<ProductResponse> list(ProductQuery search) {
        Query query = products();
        if (search.categoryId() != null) {
            query = query.whereEqualTo("categoryId", search.categoryId());
        }
        if (search.specField() != null) {
            query = query.whereEqualTo(FieldPath.of("specifications", search.specField()), search.specValue());
        }
        if (search.status() != null) {
            query = query.whereEqualTo("status", search.status());
        }
        query = query.orderBy(search.sortBy(), search.direction()).orderBy(FieldPath.documentId(), search.direction());
        if (search.afterId() != null) {
            DocumentSnapshot cursor = await(products().document(search.afterId()).get());
            if (!cursor.exists() || cursor.get(search.sortBy()) == null) {
                throw new ApplicationException(HttpStatus.BAD_REQUEST, "Pagination cursor is no longer valid");
            }
            query = query.startAfter(cursor);
        } else if (search.offset() > 0) {
            query = query.offset(search.offset());
        }
        return DocumentPage.from(await(query.limit(search.size() + 1).get()).getDocuments(), search.size(),
                document -> ProductResponse.fromEntity(document.toObject(Product.class)));
    }

    public Product get(String id) {
        return requireProduct(await(products().document(id).get()));
    }

    public Product get(Transaction transaction, String id) {
        return requireProduct(await(transaction.get(products().document(id))));
    }

    public List<ProductResponse> getAll(List<String> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        DocumentReference[] references = ids.stream()
                .map(products()::document)
                .toArray(DocumentReference[]::new);
        return await(firestore.getAll(references)).stream()
                .filter(DocumentSnapshot::exists)
                .map(document -> ProductResponse.fromEntity(document.toObject(Product.class)))
                .toList();
    }

    public Product create(ProductRequest request) {
        Product product = new Product(request);
        await(firestore.runTransaction(transaction -> {
            transaction.create(products().document(product.getId()), product);
            advanceRevisions(transaction, product.getCategoryId());
            return null;
        }));
        return product;
    }

    public Product update(String id, ProductRequest request) {
        DocumentReference reference = products().document(id);
        return await(firestore.runTransaction(transaction -> {
            Product product = requireProduct(await(transaction.get(reference)));
            String previousCategory = product.getCategoryId();
            if (product.apply(request)) {
                transaction.update(reference, fieldsForUpdate(product));
                advanceRevisions(transaction, previousCategory, product.getCategoryId());
                analytics.recordUpdate(transaction, product.getId());
            }
            return product;
        }));
    }

    public void delete(String id) {
        DocumentReference reference = products().document(id);
        await(firestore.runTransaction(transaction -> {
            Product product = requireProduct(await(transaction.get(reference)));
            transaction.delete(reference);
            advanceRevisions(transaction, product.getCategoryId());
            return null;
        }));
    }

    public long revision(String categoryId) {
        return revisions.read(scope(categoryId));
    }

    public void advanceRevisions(Transaction transaction, String... categories) {
        revisions.advance(transaction, GLOBAL_SCOPE);
        Arrays.stream(categories)
                .filter(category -> category != null && !category.isBlank())
                .map(ProductRepository::scope)
                .distinct()
                .forEach(category -> revisions.advance(transaction, category));
    }

    private CollectionReference products() {
        return firestore.collection("products");
    }

    private static Product requireProduct(DocumentSnapshot snapshot) {
        Product product = snapshot.toObject(Product.class);
        if (product == null) {
            throw new ProductNotFoundException("Product not found");
        }
        return product;
    }

    private static Map<String, Object> fieldsForUpdate(Product product) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", product.getName());
        fields.put("description", product.getDescription());
        fields.put("price", product.getPrice());
        fields.put("categoryId", product.getCategoryId());
        fields.put("specifications", product.getSpecifications());
        String canonicalStatus = product.getActualStatus().getCode();
        if (!Objects.equals(product.getStatus(), canonicalStatus)) {
            fields.put("status", canonicalStatus);
            product.setStatus(canonicalStatus);
        }
        return fields;
    }

    private static String scope(String categoryId) {
        return categoryId == null || categoryId.isBlank()
                ? GLOBAL_SCOPE : "products-category-" + StorageKey.hash(categoryId);
    }
}
