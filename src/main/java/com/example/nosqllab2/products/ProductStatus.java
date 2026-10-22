package com.example.nosqllab2.products;

import java.util.Date;

public record ProductStatus(
        String code,
        Date updatedAt,
        String comment
) {
}
