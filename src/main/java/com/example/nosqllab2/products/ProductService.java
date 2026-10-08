package com.example.nosqllab2.products;

import com.example.nosqllab2.exceptions.ProductNotFoundException;
import com.example.nosqllab2.products.cache.ProductCacheEntry;
import com.example.nosqllab2.products.cache.ProductCacheRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final Firestore firestore;
    private static final String INTERVAL_PATTERN = "yyyy-MM-dd_HH";

    private final ProductCacheRepository cacheRepository;
    private static final String CACHE_KEY_PREFIX = "products:cache:";
    private static final int CACHE_TTL_MS = 60_000;

    public List<ProductResponse> getProducts(String categoryId, String charKey, String charVal, Pageable pageable) {
        String cacheKey = String.format("cat:%s_key:%s_val:%s_p:%d_s:%d_sort:%s",
                categoryId, charKey, charVal,
                pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().toString());

        Optional<ProductCacheEntry> cachedEntry = cacheRepository.findById(cacheKey);
        if (cachedEntry.isPresent()) {
            ProductCacheEntry entry = cachedEntry.get();
            if (entry.getExpiresAt() > System.currentTimeMillis()) {
                log.info("Cache HIT in Riak KV for key: {}", cacheKey);
                return entry.getProducts();
            } else {
                log.info("Cache EXPIRED in Riak KV for key: {}", cacheKey);
                cacheRepository.delete(cacheKey);
            }
        } else {
            log.info("Cache MISS in Riak KV for key: {}", cacheKey);
        }

        List<ProductResponse> products = fetchProductsFromFirestore(categoryId, charKey, charVal, pageable);

        try {
            ProductCacheEntry newEntry = new ProductCacheEntry(
                    cacheKey,
                    products,
                    System.currentTimeMillis() + CACHE_TTL_MS
            );
            cacheRepository.save(newEntry);
        } catch (Exception e) {
            log.warn("Failed to write to Riak cache", e);
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
                return ProductResponse.fromEntity(product);
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
                DocumentSnapshot productSnap = transaction.get(productRef).get();
                DocumentSnapshot statsSnap = transaction.get(statsRef).get();
                if (!productSnap.exists()) {
                    throw new ProductNotFoundException("Product not found");
                }
                Map<String, Object> updates = new HashMap<>();
                updates.put("name", request.name());
                updates.put("description", request.description());
                updates.put("price", request.price());
                if (request.categoryId() != null) {
                    updates.put("categoryId", request.categoryId());
                }
                if (request.characteristics() != null) {
                    updates.put("characteristics", request.characteristics());
                }
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
                Object currentStatus = productSnap.get("status");
                Date createdAt = productSnap.getDate("createdAt");

                String categoryId = request.categoryId() != null
                        ? request.categoryId()
                        : productSnap.getString("categoryId");
                Map<String, Object> characteristics = request.characteristics() != null
                        ? request.characteristics()
                        : (Map<String, Object>) productSnap.get("characteristics");
                return new ProductResponse(
                        id,
                        request.name(),
                        request.description(),
                        request.price(),
                        currentStatus,
                        createdAt,
                        categoryId,
                        characteristics
                );
            }).get();
            invalidateCache();
            return response;

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
