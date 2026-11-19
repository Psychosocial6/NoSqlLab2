package com.example.nosqllab2.experiments;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@RequiredArgsConstructor
@RequestMapping("/api/experiment/consistency")
@RestController
public class ConsistencyExperimentController {

    private final CqlSession cqlSession;

    public record ExperimentResult(
            int iterations,
            double writeOneAvgMs,
            double writeQuorumAvgMs,
            double readOneAvgMs,
            double readQuorumAvgMs
    ) {}

    @GetMapping
    public ExperimentResult runTest(@RequestParam(defaultValue = "100") int iterations) {
        String userId = "test-user";
        LocalDate bucket = LocalDate.now();

        for (int i = 0; i < 10; i++) {
            executeWrite(userId, bucket, DefaultConsistencyLevel.ONE);
        }

        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            executeWrite(userId, bucket, DefaultConsistencyLevel.ONE);
        }
        double writeOneMs = (System.nanoTime() - start) / (1000000.0 * iterations);

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            executeWrite(userId, bucket, DefaultConsistencyLevel.QUORUM);
        }
        double writeQuorumMs = (System.nanoTime() - start) / (1000000.0 * iterations);

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            executeRead(userId, bucket, DefaultConsistencyLevel.ONE);
        }
        double readOneMs = (System.nanoTime() - start) / (1000000.0 * iterations);

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            executeRead(userId, bucket, DefaultConsistencyLevel.QUORUM);
        }
        double readQuorumMs = (System.nanoTime() - start) / (1000000.0 * iterations);

        return new ExperimentResult(iterations, writeOneMs, writeQuorumMs, readOneMs, readQuorumMs);
    }

    @GetMapping("/availability")
    public String testAvailability(@RequestParam DefaultConsistencyLevel cl) {
        try {
            executeWrite("test-user", LocalDate.now(), cl);
            executeRead("test-user", LocalDate.now(), cl);
            return "SUCCESS with " + cl;
        } catch (Exception e) {
            return "FAILED with " + cl + ": " + e.getMessage();
        }
    }

    private void executeWrite(String userId, LocalDate bucket, DefaultConsistencyLevel cl) {
        SimpleStatement stmt = SimpleStatement.builder(
                        "INSERT INTO events_by_user_month (user_id, month_bucket, event_time, event_id, message) VALUES (?, ?, ?, ?, ?)")
                .addPositionalValues(userId, bucket, Instant.now(), UUID.randomUUID(), "test-message")
                .setConsistencyLevel(cl)
                .build();
        cqlSession.execute(stmt);
    }

    private void executeRead(String userId, LocalDate bucket, DefaultConsistencyLevel cl) {
        SimpleStatement stmt = SimpleStatement.builder(
                        "SELECT * FROM events_by_user_month WHERE user_id = ? AND month_bucket = ? LIMIT 1")
                .addPositionalValues(userId, bucket)
                .setConsistencyLevel(cl)
                .build();
        cqlSession.execute(stmt);
    }
}