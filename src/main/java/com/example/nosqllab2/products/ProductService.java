package com.example.nosqllab2.products;

import com.example.nosqllab2.products.cache.CacheIndexEntry;
import com.example.nosqllab2.products.cache.CacheIndexRepository;
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
    private final CacheIndexRepository indexRepository;

    private static final long CACHE_TTL_MS = 60_000;

    public List<ProductResponse> getProducts(String categoryId, String specField, String specVal, String statusFilter, Pageable pageable) {
        String normalizedCatId = (categoryId != null && !categoryId.isBlank()) ? categoryId : "all";

        String sortStr = pageable.getSort().isSorted()
                ? pageable.getSort().stream()
                .map(order -> order.getProperty() + ":" + order.getDirection().name())
                .collect(Collectors.joining(","))
                : "UNSORTED";

        String cacheKey = String.format("cat:%s_f:%s_v:%s_st:%s_p:%d_s:%d_sort:%s",
                normalizedCatId, specField, specVal, statusFilter,
                pageable.getPageNumber(), pageable.getPageSize(),
                sortStr);

        Optional<ProductCacheEntry> cachedEntry = cacheRepository.findById(cacheKey);

        if (cachedEntry.isPresent()) {
            ProductCacheEntry entry = cachedEntry.get();
            if (entry.getExpiresAt() > System.currentTimeMillis()) {
                log.info("Cache HIT in Riak KV for key: {}", cacheKey);
                return entry.getProducts();
            } else {
                log.info("Cache EXPIRED in Riak KV for key: {}", cacheKey);
                cacheRepository.delete(cacheKey);
                removeKeyFromIndex(normalizedCatId, cacheKey);
            }
        } else {
            log.info("Cache MISS in Riak KV for key: {}", cacheKey);
        }

        List<ProductResponse> products = fetchProductsFromFirestore(categoryId, specField, specVal, statusFilter, pageable);

        try {
            ProductCacheEntry newEntry = new ProductCacheEntry(
                    cacheKey,
                    normalizedCatId,
                    products,
                    System.currentTimeMillis() + CACHE_TTL_MS
            );
            cacheRepository.save(newEntry);

            addKeyToIndex(normalizedCatId, cacheKey);
            if (!"all".equals(normalizedCatId)) {
                addKeyToIndex("all", cacheKey);
            }

        } catch (Exception e) {
            log.error("CRITICAL: Failed to write to Riak cache for key: " + cacheKey, e);
        }

        return products;
    }

    private List<ProductResponse> fetchProductsFromFirestore(String categoryId, String specField, String specVal, String statusFilter, Pageable pageable) {
        try {
            CollectionReference productsRef = firestore.collection("products");
            Query query = productsRef;

            if (categoryId != null && !categoryId.isBlank()) {
                query = query.whereEqualTo("categoryId", categoryId);
            }

            if (statusFilter != null && !statusFilter.isBlank()) {
                query = query.whereEqualTo("status", statusFilter);
            }

            if (specField != null && !specField.isBlank() && specVal != null && !specVal.isBlank()) {
                query = query.whereEqualTo("specifications." + specField, specVal);
            }

            if (pageable.getSort().isSorted()) {
                for (Sort.Order order : pageable.getSort()) {
                    Query.Direction direction = order.isAscending() ? Query.Direction.ASCENDING : Query.Direction.DESCENDING;
                    query = query.orderBy(order.getProperty(), direction);
                }
            } else {
                if (statusFilter != null && !statusFilter.isBlank()) {
                    query = query.orderBy("createdAt", Query.Direction.DESCENDING);
                } else {
                    query = query.orderBy("name", Query.Direction.ASCENDING);
                }
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
                        product.getSpecifications()
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
        product.setSpecifications(productRequest.specifications());

        Product saved = productRepository.save(product).block();

        invalidateCacheForCategory(productRequest.categoryId());

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
                updates.put("specifications", request.specifications());
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

                ProductStatus status;
                if (snapshot.contains("updatedStatus") && snapshot.get("updatedStatus") != null) {
                    Map<String, Object> statusMap = (Map<String, Object>) snapshot.get("updatedStatus");
                    Date updatedAt = snapshot.getDate("updatedStatus.updatedAt");
                    status = new ProductStatus(
                            (String) statusMap.get("code"),
                            updatedAt,
                            (String) statusMap.get("comment")
                    );
                } else {
                    String oldStatus = snapshot.getString("status");
                    status = new ProductStatus(
                            oldStatus,
                            Date.from(Instant.EPOCH),
                            "not updated status"
                    );
                }
                Date createdAt = snapshot.getDate("createdAt");

                return new ProductResponse(
                        id,
                        request.name(),
                        request.description(),
                        request.price(),
                        status,
                        createdAt,
                        request.categoryId(),
                        request.specifications()
                );
            }).get();

            invalidateCacheForCategory(request.categoryId());
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

        String categoryId = product.getCategoryId();
        productRepository.delete(product).block();

        invalidateCacheForCategory(categoryId);
    }


    private String getIndexKey(String categoryId) {
        String cat = (categoryId != null && !categoryId.isBlank()) ? categoryId : "all";
        return "idx:cat:" + cat;
    }

    private synchronized void addKeyToIndex(String categoryId, String cacheKey) {
        String indexKey = getIndexKey(categoryId);
        try {
            Optional<CacheIndexEntry> indexEntry = indexRepository.findById(indexKey);
            CacheIndexEntry entry = indexEntry.orElseGet(() -> new CacheIndexEntry(indexKey, new HashSet<>()));

            entry.getCacheKeys().add(cacheKey);
            indexRepository.save(entry);

        } catch (Exception e) {
            log.warn("Failed to update cache index for category: {}", categoryId, e);
        }
    }

    private synchronized void removeKeyFromIndex(String categoryId, String cacheKey) {
        String indexKey = getIndexKey(categoryId);
        try {
            Optional<CacheIndexEntry> indexEntry = indexRepository.findById(indexKey);
            if (indexEntry.isPresent()) {
                CacheIndexEntry entry = indexEntry.get();
                entry.getCacheKeys().remove(cacheKey);
                indexRepository.save(entry);
            }
        } catch (Exception e) {
            log.warn("Failed to remove key from cache index for category: {}", categoryId, e);
        }
    }

    private void invalidateCacheForCategory(String categoryId) {
        String targetCat = (categoryId != null && !categoryId.isBlank()) ? categoryId : "all";

        clearIndexAndAssociatedCache(targetCat);

        if (!"all".equals(targetCat)) {
            clearIndexAndAssociatedCache("all");
        }
    }

    private void clearIndexAndAssociatedCache(String categoryId) {
        String indexKey = getIndexKey(categoryId);
        try {
            Optional<CacheIndexEntry> indexEntry = indexRepository.findById(indexKey);
            if (indexEntry.isPresent() && indexEntry.get().getCacheKeys() != null) {
                for (String actualCacheKey : indexEntry.get().getCacheKeys()) {
                    cacheRepository.delete(actualCacheKey);
                    log.info("Granular cache deleted for key: {}", actualCacheKey);
                }
                indexRepository.delete(indexKey);
                log.info("Cache index deleted for category/scope: {}", categoryId);
            }
        } catch (Exception e) {
            log.warn("Failed to clear cache index for category: {}", categoryId, e);
        }
    }
}