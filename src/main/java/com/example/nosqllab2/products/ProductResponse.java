package com.example.nosqllab2.products;

import java.util.Date;

public record ProductResponse(
        String id,
        String name,
        String description,
        Double price,
        ProductStatus status,
        Date createdAt,
        String categoryId,
        ProductSpecifications specifications
) {
    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getActualStatus(),
                product.getCreatedAt(),
                product.getCategoryId(),
                product.getSpecifications()
        );
    }
}