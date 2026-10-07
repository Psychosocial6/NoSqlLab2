package com.example.nosqllab2.favorites;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface UserFavoriteRepository extends FirestoreReactiveRepository<UserFavorite> {
    Flux<UserFavorite> findByUserId(String userId);
}