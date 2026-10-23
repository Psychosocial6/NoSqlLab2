package com.example.nosqllab2.products;

import com.basho.riak.client.api.RiakClient;
import com.example.nosqllab2.products.cache.ProductCacheEntry;
import com.example.nosqllab2.products.cache.ProductCacheRepository;
import com.example.nosqllab2.exceptions.ProductNotFoundException;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final Firestore firestore;
    private static final String INTERVAL_PATTERN = "yyyy-MM-dd_HH";

    private final ProductCacheRepository cacheRepository;

    public List<ProductResponse> getProducts(String categoryId, String charKey, String charVal, Pageable pageable) {
        String sortStr = pageable.getSort().isSorted()
                ? pageable.getSort().stream()
                .map(order -> order.getProperty() + ":" + order.getDirection().name())
                .collect(Collectors.joining(","))
                : "UNSORTED";

        String cacheKey = String.format("cat:%s_key:%s_val:%s_p:%d_s:%d_sort:%s",
                categoryId, charKey, charVal,
                pageable.getPageNumber(), pageable.getPageSize(),
                sortStr);

        Optional<ProductCacheEntry> cachedEntry = cacheRepository.findById(cacheKey);

        if (cachedEntry.isPresent()) {
            log.info("Cache HIT in Riak KV for key: {}", cacheKey);
            return cachedEntry.get().getProducts();
        }

        log.info("Cache MISS in Riak KV for key: {}", cacheKey);

        List<ProductResponse> products = fetchProductsFromFirestore(categoryId, charKey, charVal, pageable);

        try {
            ProductCacheEntry newEntry = new ProductCacheEntry(
                    cacheKey,
                    products
            );
            cacheRepository.save(newEntry);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to write to Riak cache for key: " + cacheKey, e); // Логируем ERROR с трейсом
        }

        return products;
    }

    private List<ProductResponse> fetchProductsFromFirestore(String categoryId, String charKey, String charVal, Pageable pageable) {
        try {
            CollectionReference productsRef = firestore.collection("products");
            Query query = productsRef;

            if (categoryId != null && !categoryId.isBlank()) {
                query = query.whereEqualTo("categoryId", categoryId);
            }
            if (charKey != null && !charKey.isBlank() && charVal != null && !charVal.isBlank()) {
                query = query.whereEqualTo("characteristics." + charKey, charVal);
            }

            if (pageable.getSort().isSorted()) {
                for (Sort.Order order : pageable.getSort()) {
                    Query.Direction direction = order.isAscending() ? Query.Direction.ASCENDING : Query.Direction.DESCENDING;
                    query = query.orderBy(order.getProperty(), direction);
                }
            } else {
                query = query.orderBy("name", Query.Direction.ASCENDING);
            }

            query = query.limit(pageable.getPageSize());
            if (pageable.getPageNumber() > 0) {
                query = query.offset(pageable.getPageNumber() * pageable.getPageSize());
            }

            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

            return documents.stream().map(doc -> {
                Product product = doc.toObject(Product.class);
                ProductStatus status = Optional.ofNullable(doc.get("updatedStatus", ProductStatus.class))
                        .orElseGet(product::getActualStatus);

                return new ProductResponse(
                        doc.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        status,
                        product.getCreatedAt(),
                        product.getCategoryId(),
                        product.getCharacteristics()
                );
            }).toList();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Query was interrupted", e);
        } catch (ExecutionException e) {
            log.error("Firestore error cause: ", e.getCause());
            throw new RuntimeException("Error executing Firestore query: " + e.getCause().getMessage(), e);
        }
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
        product.setCategoryId(productRequest.categoryId());
        product.setCharacteristics(productRequest.characteristics());

        Product saved = productRepository.save(product).block();
        invalidateCache();
        return ProductResponse.fromEntity(saved);
    }

    public ProductResponse updateProduct(String id, ProductRequest request) {
        DocumentReference productRef = firestore.collection("products").document(id);

        SimpleDateFormat sdf = new SimpleDateFormat(INTERVAL_PATTERN);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String currentIntervalKey = sdf.format(new Date());
        DocumentReference statsRef = firestore.collection("product_change_stats").document(currentIntervalKey);

        try {
            ProductResponse response = firestore.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(productRef).get();
                if (!snapshot.exists()) {
                    throw new ProductNotFoundException("Product not found");
                }
                DocumentSnapshot statsSnap = transaction.get(statsRef).get();

                Map<String, Object> updates = new HashMap<>();
                updates.put("name", request.name());
                updates.put("description", request.description());
                updates.put("price", request.price());
                updates.put("categoryId", request.categoryId());
                updates.put("characteristics", request.characteristics());
                transaction.update(productRef, updates);

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

                ProductStatus status = Optional.ofNullable(snapshot.get("updatedStatus", ProductStatus.class))
                        .orElseGet(() -> new ProductStatus(
                                snapshot.getString("status"),
                                Date.from(Instant.EPOCH),
                                "not updated status"
                        ));
                Date createdAt = snapshot.getDate("createdAt");

                return new ProductResponse(
                        id,
                        request.name(),
                        request.description(),
                        request.price(),
                        status,
                        createdAt,
                        request.categoryId(),
                        request.characteristics()
                );
            }).get();
            invalidateCache();
            return response;

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to update product: " + e.getMessage(), e);
        }
    }

    public void deleteProductById(String id) {
        Product product = productRepository.findById(id).block();
        if (product == null) {
            throw new ProductNotFoundException("Product not found");
        }
        invalidateCache();
        productRepository.delete(product).block();
    }

    private void invalidateCache() {
        try {
            List<ProductCacheEntry> allCached = cacheRepository.findAll();
            for (ProductCacheEntry entry : allCached) {
                cacheRepository.delete(entry.getCacheKey());
            }
            log.info("Riak cache successfully invalidated.");
        } catch (Exception e) {
            log.warn("Failed to invalidate cache in Riak", e);
        }
    }
}