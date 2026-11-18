package com.example.nosqllab2.operations;

import com.example.nosqllab2.repository.VersionedCacheEntry;
import com.example.nosqllab2.storage.DocumentPage;

public record OperationCache(
        String cacheKey,
        long revision,
        long expiresAt,
        DocumentPage<OperationLogDTO> page
) implements VersionedCacheEntry {
}
