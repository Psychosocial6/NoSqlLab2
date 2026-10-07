package com.example.nosqllab2.users;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends FirestoreReactiveRepository<User> {
    Mono<User> findByNameIgnoreCase(String name);
    Mono<Boolean> existsByNameIgnoreCase(String name);
}