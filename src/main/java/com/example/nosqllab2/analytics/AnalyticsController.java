package com.example.nosqllab2.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
@RequestMapping("/api/analytics")
@RestController
public class AnalyticsController {

    private final ProductAnalyticsService analyticsService;

    @GetMapping("/changes")
    public ResponseEntity<List<ChangeIntervalStat>> getAnalytics(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) throws ExecutionException, InterruptedException {
        return ResponseEntity.ok(analyticsService.getChangeAnalytics(from, to));
    }
}