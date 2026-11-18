package com.example.nosqllab2.products;

import com.example.nosqllab2.products.cache.ProductCacheEntry;
import com.example.nosqllab2.products.cache.ProductCacheRepository;
import com.example.nosqllab2.storage.DocumentPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ProductService {
    private final ProductRepository products;
    private final ProductCacheRepository cache;

    public DocumentPage<ProductResponse> getProducts(ProductQuery query) {
        long revision = products.revision(query.categoryId());
        String key = query.cacheKey();
        return cache.findCurrent(key, revision)
                .map(ProductCacheEntry::page)
                .orElseGet(() -> {
                    DocumentPage<ProductResponse> page = products.list(query);
                    cache.saveBestEffort(new ProductCacheEntry(key, revision, cache.expiryTime(), page));
                    return page;
                });
    }

    public ProductResponse getProductById(String id) {
        return ProductResponse.fromEntity(products.get(id));
    }

    public ProductResponse createProduct(ProductRequest request) {
        return ProductResponse.fromEntity(products.create(request));
    }

    public ProductResponse updateProduct(String id, ProductRequest request) {
        return ProductResponse.fromEntity(products.update(id, request));
    }

    public void deleteProductById(String id) {
        products.delete(id);
    }
}
