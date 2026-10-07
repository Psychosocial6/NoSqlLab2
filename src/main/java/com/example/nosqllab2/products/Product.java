package com.example.nosqllab2.products;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;


@NoArgsConstructor
@Setter
@Getter
@Document(collectionName = "products")
public class Product {
    @DocumentId
    private String id;
    private String name;
    private String description;
    private Double price;

    public Product(String name, String description, Double price) {
        this.id = String.valueOf(UUID.randomUUID());
        this.name = name;
        this.description = description;
        this.price = price;
    }
}
