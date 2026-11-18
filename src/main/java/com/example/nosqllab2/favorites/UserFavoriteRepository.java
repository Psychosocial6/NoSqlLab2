package com.example.nosqllab2.favorites;

import com.example.nosqllab2.exceptions.ApplicationException;
import com.example.nosqllab2.operations.OperationLogRepository;
import com.example.nosqllab2.products.Product;
import com.example.nosqllab2.products.ProductRepository;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import static com.example.nosqllab2.storage.FirestoreCalls.await;

@Repository
@RequiredArgsConstructor
public class UserFavoriteRepository {
    private final Firestore firestore;
    private final ProductRepository products;
    private final OperationLogRepository operations;

    public List<String> findProductIds(String userId) {
        return await(favorites().whereEqualTo("userId", userId).select("productId").get()).getDocuments().stream()
                .map(document -> document.getString("productId"))
                .filter(id -> id != null && !id.isBlank())
                .toList();
    }

    public boolean add(String userId, String productId) {
        DocumentReference reference = reference(userId, productId);
        return await(firestore.runTransaction(transaction -> {
            if (await(transaction.get(reference)).exists()) {
                return false;
            }
            Product product = products.get(transaction, productId);
            if (!Product.AVAILABLE.equalsIgnoreCase(product.getActualStatus().getCode())) {
                throw new ApplicationException(HttpStatus.CONFLICT, "Product is unavailable");
            }
            transaction.create(reference, Map.of("userId", userId, "productId", productId));
            operations.append(transaction, userId, "User (id=%s) added product (id=%s) to favs"
                    .formatted(userId, productId));
            return true;
        }));
    }

    public void delete(String userId, String productId) {
        DocumentReference reference = reference(userId, productId);
        await(firestore.runTransaction(transaction -> {
            if (await(transaction.get(reference)).exists()) {
                transaction.delete(reference);
                operations.append(transaction, userId, "User (id=%s) deleted product (id=%s) from favs"
                        .formatted(userId, productId));
            }
            return null;
        }));
    }

    private DocumentReference reference(String userId, String productId) {
        return favorites().document(userId + "_" + productId);
    }

    private CollectionReference favorites() {
        return firestore.collection("user_favorites");
    }
}
