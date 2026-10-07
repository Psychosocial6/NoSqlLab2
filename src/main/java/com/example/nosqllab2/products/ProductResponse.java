package com.example.nosqllab2.products;

import java.math.BigDecimal;

public record ProductResponse(String id, String name, String description, BigDecimal price) {
    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice());
    }
}
