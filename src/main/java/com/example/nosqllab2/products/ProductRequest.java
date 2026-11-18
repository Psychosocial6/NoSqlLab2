package com.example.nosqllab2.products;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank @Size(max = 256) String name,
        @NotBlank @Size(max = 4000) String description,
        @NotNull @Positive @DecimalMax("1E15") Double price,
        @Size(max = 256) String categoryId,
        @Valid ProductSpecifications specifications
) {
    public ProductRequest {
        categoryId = categoryId == null || categoryId.isBlank() ? null : categoryId.strip();
    }
}
