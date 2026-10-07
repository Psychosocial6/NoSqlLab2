package com.example.nosqllab2.operations;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Document
public class OperationLog {
    @DocumentId
    private String id;
    private String userId;
    private String operation;
    private LocalDateTime operationTime;

    public OperationLog(String userId, String operation, LocalDateTime operationTime) {
        this.userId = userId;
        this.operation = operation;
        this.operationTime = operationTime;
    }
}