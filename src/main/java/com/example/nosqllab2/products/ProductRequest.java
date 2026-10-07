package com.example.nosqllab2.products;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductRequest(
        @NotEmpty(message = "name required")
        String name,
        @NotEmpty(message = "description required")
        String description,
        @NotNull(message = "price required")
        @Positive(message = "price must be positive")
        BigDecimal price
) {
}

