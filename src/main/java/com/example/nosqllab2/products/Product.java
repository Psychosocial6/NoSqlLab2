package com.example.nosqllab2.products;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.Exclude;
import com.google.cloud.spring.data.firestore.Document;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
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
    private String status;
    private ProductStatus updatedStatus;
    private Date createdAt;

    private String categoryId;
    private ProductSpecifications specifications;

    public Product(String name, String description, Double price) {
        this.id = String.valueOf(UUID.randomUUID());
        this.name = name;
        this.description = description;
        this.price = price;
        this.status = "AVAILABLE";
        this.createdAt = new Date();
    }

    @Exclude
    public ProductStatus getActualStatus() {
        if (updatedStatus != null) {
            return updatedStatus;
        }
        return new ProductStatus(
                status != null ? status : "AVAILABLE",
                Date.from(Instant.EPOCH),
                "not updated status"
        );
    }
}