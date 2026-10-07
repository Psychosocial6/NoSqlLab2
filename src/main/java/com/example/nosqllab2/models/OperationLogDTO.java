package com.example.nosqllab2.models;

import com.example.nosqllab2.operations.OperationLog;

public record OperationLogDTO(
        String id,
        String userId,
        String operation,
        String operationTime
) {
    public static OperationLogDTO fromEntity(OperationLog entity) {
        return new OperationLogDTO(
                String.valueOf(entity.getId()),
                entity.getUserId(),
                entity.getOperation(),
                entity.getOperationTime() != null ? entity.getOperationTime().toString() : ""
        );
    }
}