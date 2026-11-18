package com.example.nosqllab2.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/analytics")
@RestController
public class AnalyticsController {
    private final ProductAnalyticsService analyticsService;

    @GetMapping("/changes")
    public List<ChangeIntervalStat> getAnalytics(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return analyticsService.getChangeAnalytics(from, to);
    }
}
