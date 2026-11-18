package com.example.nosqllab2.repository;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.core.query.Namespace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Duration;
import java.util.Optional;

@Slf4j
public abstract class RiakCacheRepository<T extends VersionedCacheEntry> extends RiakRepository<T> {
    private static final long TTL_MILLIS = Duration.ofMinutes(1).toMillis();

    protected RiakCacheRepository(RiakClient client, String bucket, Class<T> entryClass, Duration timeout) {
        super(client, new Namespace(bucket), entryClass, timeout);
    }

    public Optional<T> findCurrent(String key, long revision) {
        try {
            return findById(key).filter(entry -> entry.revision() == revision
                    && entry.expiresAt() > System.currentTimeMillis());
        } catch (DataAccessResourceFailureException exception) {
            handleCacheFailure("read", exception);
            return Optional.empty();
        }
    }

    public void saveBestEffort(T entry) {
        try {
            save(entry);
        } catch (DataAccessResourceFailureException exception) {
            handleCacheFailure("write", exception);
        }
    }

    public long expiryTime() {
        return System.currentTimeMillis() + TTL_MILLIS;
    }

    @Override
    protected String extractId(T entity) {
        return entity.cacheKey();
    }

    private static void handleCacheFailure(String operation, DataAccessResourceFailureException exception) {
        if (Thread.currentThread().isInterrupted()) {
            throw exception;
        }
        log.warn("Riak cache {} failed: {}", operation, exception.getMessage());
    }
}
