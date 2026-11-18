package com.example.nosqllab2.products.cache;

import com.example.nosqllab2.products.ProductResponse;
import com.example.nosqllab2.repository.VersionedCacheEntry;
import com.example.nosqllab2.storage.DocumentPage;

public record ProductCacheEntry(
        String cacheKey,
        long revision,
        long expiresAt,
        DocumentPage<ProductResponse> page
) implements VersionedCacheEntry {
}
