package com.example.nosqllab2.categories;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends FirestoreReactiveRepository<Category> {
}
