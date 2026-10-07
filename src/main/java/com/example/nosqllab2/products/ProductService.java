package com.example.nosqllab2.products;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final Firestore firestore;
    private static final String INTERVAL_PATTERN = "yyyy-MM-dd_HH";

    public List<ProductResponse> getProducts() {
        return productRepository
                .findAll()
                .collectList()
                .block()
                .stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id).block();
        if (product == null) {
            throw new ProductNotFoundException("Product not found");
        }
        return ProductResponse.fromEntity(product);
    }


    public ProductResponse createProduct(ProductRequest productRequest) {
        Product product = new Product(
                productRequest.name(),
                productRequest.description(),
                productRequest.price()
        );
        Product saved = productRepository.save(product).block();
        return ProductResponse.fromEntity(saved);
    }

    public ProductResponse updateProduct(String id, ProductRequest request) {
        DocumentReference productRef = firestore.collection("products").document(id);

        SimpleDateFormat sdf = new SimpleDateFormat(INTERVAL_PATTERN);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String currentIntervalKey = sdf.format(new Date());
        DocumentReference statsRef = firestore.collection("product_change_stats").document(currentIntervalKey);

        try {
            return firestore.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(productRef).get();
                if (!snapshot.exists()) {
                    throw new ProductNotFoundException("Product not found");
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("name", request.name());
                updates.put("description", request.description());
                updates.put("price", request.price());
                transaction.update(productRef, updates);

                DocumentSnapshot statsSnap = transaction.get(statsRef).get();
                if (!statsSnap.exists()) {
                    Map<String, Object> initialStat = new HashMap<>();
                    initialStat.put("intervalKey", currentIntervalKey);
                    initialStat.put("changeCount", 1L);
                    initialStat.put("lastUpdated", new Date());
                    transaction.set(statsRef, initialStat);
                } else {
                    transaction.update(statsRef,
                            "changeCount", FieldValue.increment(1),
                            "lastUpdated", new Date()
                    );
                }

                log.info("Product [{}] updated and analytics [{}] incremented", id, currentIntervalKey);

                Object currentStatus = snapshot.get("status");
                Date createdAt = snapshot.getDate("createdAt");

                return new ProductResponse(
                        id,
                        request.name(),
                        request.description(),
                        request.price(),
                        currentStatus,
                        createdAt
                );
            }).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    public void deleteProductById(String id) {
        Product product = productRepository.findById(id).block();
        if (product == null) {
            throw new ProductNotFoundException("Product not found");
        }

        productRepository.delete(product).block();
    }
}
