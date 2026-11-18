package com.example.nosqllab2.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.TreeMap;

@RequiredArgsConstructor
@Service
public class ProductAnalyticsService {
    private final ProductChangeRepository repository;

    public List<ChangeIntervalStat> getChangeAnalytics(String fromInterval, String toInterval) {
        var byInterval = new TreeMap<String, ProductChangeRepository.IntervalShard>();
        for (var shard : repository.read(ChangeIntervals.range(fromInterval, toInterval))) {
            byInterval.merge(shard.interval(), shard, ProductChangeRepository.IntervalShard::combine);
        }
        return byInterval.values().stream()
                .map(stat -> new ChangeIntervalStat(stat.interval(), stat.count(),
                        stat.lastUpdated().equals(Instant.EPOCH) ? "" : stat.lastUpdated().toString()))
                .toList();
    }
}
