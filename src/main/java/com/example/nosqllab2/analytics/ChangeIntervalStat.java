package com.example.nosqllab2.analytics;

public record ChangeIntervalStat(
        String intervalKey,
        Long changeCount,
        String lastUpdated
) {}