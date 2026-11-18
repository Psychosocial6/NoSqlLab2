package com.example.nosqllab2.operations;

import com.example.nosqllab2.exceptions.ApplicationException;
import com.example.nosqllab2.storage.DocumentPage;
import com.example.nosqllab2.storage.StorageKey;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class OperationService {
    static final int DEFAULT_PAGE_SIZE = 100;
    private final OperationLogRepository logs;
    private final OperationCacheRepository cache;

    public DocumentPage<OperationLogDTO> getUserOperations(String userId, int size, String cursor) {
        String after = DocumentPage.decodeCursor(cursor);
        long revision = logs.revision(userId);
        String key = cacheKey(userId, size, after);
        return cache.findCurrent(key, revision).map(OperationCache::page).orElseGet(() -> {
            DocumentPage<OperationLogDTO> page = logs.list(userId, size, after);
            cache.saveBestEffort(new OperationCache(key, revision, cache.expiryTime(), page));
            return page;
        });
    }

    public Benchmark speedtest(String userId, int iterations) {
        getUserOperations(userId, DEFAULT_PAGE_SIZE, null);
        String key = cacheKey(userId, DEFAULT_PAGE_SIZE, null);
        long revision = logs.revision(userId);
        double firestore = averageMillis(iterations, () -> logs.list(userId, DEFAULT_PAGE_SIZE, null));
        double riak = averageMillis(iterations, () -> cache.findCurrent(key, revision)
                .orElseThrow(() -> new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Riak has no current cached page for the benchmark")));
        return new Benchmark(iterations, firestore, riak, firestore / riak);
    }

    private static double averageMillis(int iterations, Runnable read) {
        long start = System.nanoTime();
        for (int readNumber = 0; readNumber < iterations; readNumber++) {
            read.run();
        }
        return (System.nanoTime() - start) / (1_000_000.0 * iterations);
    }

    private static String cacheKey(String userId, int size, String after) {
        return StorageKey.hash(StorageKey.hash(userId) + ":" + size + ":" + (after == null ? "" : after));
    }

    public record Benchmark(int iterations, double firestoreAvgMs, double riakAvgMs, double speedup) {
    }
}
