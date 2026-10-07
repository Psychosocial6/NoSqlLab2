package com.example.nosqllab2.products;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, String description, BigDecimal price) {
    public static ProductResponse fromEntity(ProductEntity productEntity) {
        return new ProductResponse(
                productEntity.getId(),
                productEntity.getName(),
                productEntity.getDescription(),
                productEntity.getPrice());
    }
}
