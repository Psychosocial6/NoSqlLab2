package com.example.nosqllab2.operations;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "operation_logs", indexes = {
        @Index(name = "idx_op_logs_user_time", columnList = "user_id, operation_time DESC")
})
public class OperationLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "operation", nullable = false)
    private String operation;

    @Column(name = "operation_time", nullable = false)
    private LocalDateTime operationTime;

    public OperationLogEntity(Long userId, String operation, LocalDateTime operationTime) {
        this.userId = userId;
        this.operation = operation;
        this.operationTime = operationTime;
    }
}