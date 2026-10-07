package com.example.nosqllab2.models;

import com.example.nosqllab2.operations.OperationLogEntity;

public record OperationLog(
        String id,
        Long userId,
        String operation,
        String operationTime
) {
    public static OperationLog fromEntity(OperationLogEntity entity) {
        return new OperationLog(
                String.valueOf(entity.getId()),
                entity.getUserId(),
                entity.getOperation(),
                entity.getOperationTime().toString()
        );
    }
}