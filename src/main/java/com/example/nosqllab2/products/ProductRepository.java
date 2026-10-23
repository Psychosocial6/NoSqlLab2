package com.example.nosqllab2.products;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends FirestoreReactiveRepository<Product> {
}
