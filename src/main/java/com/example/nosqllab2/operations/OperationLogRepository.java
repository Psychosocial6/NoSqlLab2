package com.example.nosqllab2.operations;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;


@Repository
public interface OperationLogRepository extends FirestoreReactiveRepository<OperationLog> {
    Flux<OperationLog> findByUserIdOrderByOperationTimeDesc(String userId);
}