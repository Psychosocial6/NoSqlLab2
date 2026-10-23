package com.example.nosqllab2.models;

import java.util.List;

public record OperationCache(
        String userId,
        List<OperationLogDTO> operations
) {}