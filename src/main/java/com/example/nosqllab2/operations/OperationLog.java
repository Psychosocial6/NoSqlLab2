package com.example.nosqllab2.operations;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class OperationLog {
    @DocumentId
    private String id;
    private String userId;
    private String operation;
    private Date operationTime;
}
