package com.example.nosqllab2.operations;

public record OperationLogDTO(String id, String userId, String operation, String operationTime) {
    static OperationLogDTO fromEntity(OperationLog entity) {
        return new OperationLogDTO(entity.getId(), entity.getUserId(), entity.getOperation(),
                entity.getOperationTime() == null ? "" : entity.getOperationTime().toInstant().toString());
    }
}
