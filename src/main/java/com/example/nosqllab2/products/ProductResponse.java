package com.example.nosqllab2.products;

import java.util.Date;
import java.util.Map;

public record ProductResponse(
        String id,
        String name,
        String description,
        Double price,
        Object status,
        Date createdAt,
        String categoryId,
        Map<String, Object> characteristics
) {
    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getCategoryId(),
                product.getCharacteristics()
        );
    }
}