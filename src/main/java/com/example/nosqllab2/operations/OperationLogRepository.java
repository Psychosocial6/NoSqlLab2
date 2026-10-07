package com.example.nosqllab2.operations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLogEntity, Long> {
    List<OperationLogEntity> findTop10ByUserIdOrderByOperationTimeDesc(Long userId);
}