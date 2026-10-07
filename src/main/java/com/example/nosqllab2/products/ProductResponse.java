package com.example.nosqllab2.products;


public record ProductResponse(String id, String name, String description, Double price) {
    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice());
    }
}
