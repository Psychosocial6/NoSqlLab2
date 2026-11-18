package com.example.nosqllab2.repository;

public interface VersionedCacheEntry {
    String cacheKey();

    long revision();

    long expiresAt();
}
