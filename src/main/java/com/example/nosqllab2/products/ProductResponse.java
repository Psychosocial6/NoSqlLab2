package com.example.nosqllab2.products;

import java.util.Date;

public record ProductResponse(
        String id,
        String name,
        String description,
        Double price,
        Object status,
        Date createdAt
) {
    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStatus(),
                product.getCreatedAt()
        );
    }
}