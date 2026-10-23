package com.example.nosqllab2.operations;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@Document(collectionName = "operation_logs")
public class OperationLog {
    @DocumentId
    private String id;
    private String userId;
    private String operation;
    private Date operationTime;

    public OperationLog(String userId, String operation, Date operationTime) {
        this.id = String.valueOf(UUID.randomUUID());
        this.userId = userId;
        this.operation = operation;
        this.operationTime = operationTime;
    }
}