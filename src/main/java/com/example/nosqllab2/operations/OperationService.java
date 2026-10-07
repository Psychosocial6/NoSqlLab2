package com.example.nosqllab2.operations;

import com.example.nosqllab2.models.OperationCache;
import com.example.nosqllab2.models.OperationLog;
import com.example.nosqllab2.repository.OperationCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class OperationService {

    private final OperationLogRepository logRepository;
    private final OperationCacheRepository cacheRepository;

    public void logOperation(Long userId, String operation) {
        OperationLogEntity entity = new OperationLogEntity(userId, operation, LocalDateTime.now());
        logRepository.save(entity);
        cacheRepository.delete(String.valueOf(userId));
        log.info("cache invalidated for user {}", userId);
    }

    public List<OperationLog> getUserOperations(Long userId) {
        String cacheKey = String.valueOf(userId);
        long startRiak = System.nanoTime();
        Optional<OperationCache> cache = cacheRepository.findById(cacheKey);
        long riakDurationNs = System.nanoTime() - startRiak;
        if (cache.isPresent()) {
            log.info("cache hit: {} ms ({} ns)", riakDurationNs / 1_000_000.0, riakDurationNs);
            return cache.get().operations();
        }
        long startPostgres = System.nanoTime();
        List<OperationLog> userOps = logRepository.findTop10ByUserIdOrderByOperationTimeDesc(userId)
                .stream()
                .map(OperationLog::fromEntity)
                .toList();
        long pgDurationNs = System.nanoTime() - startPostgres;
        log.info("cache miss: {} ms ({} ns)", pgDurationNs / 1_000_000.0, pgDurationNs);
        cacheRepository.save(new OperationCache(cacheKey, userOps));
        return userOps;
    }

    public Map<String, Object> speedtest(Long userId, int iterations) {
        String cacheKey = String.valueOf(userId);
        getUserOperations(userId);
        long startPg = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            logRepository.findTop10ByUserIdOrderByOperationTimeDesc(userId);
        }
        long totalPgNs = System.nanoTime() - startPg;
        double avgPgMs = (totalPgNs / 1_000_000.0) / iterations;
        long startRiak = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            cacheRepository.findById(cacheKey);
        }
        long totalRiakNs = System.nanoTime() - startRiak;
        double avgRiakMs = (totalRiakNs / 1_000_000.0) / iterations;
        double speedup = avgPgMs / avgRiakMs;
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("iterations", iterations);
        report.put("postgreSQL_avg", String.format("%.3f ms", avgPgMs));
        report.put("Riak_avg", String.format("%.3f ms", avgRiakMs));
        report.put("postgreSQL_avg / Riak_avg", String.format("%.2fx", speedup));
        log.info("Speedtest results: {}", report);
        return report;
    }
}