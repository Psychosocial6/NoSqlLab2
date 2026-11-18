package com.example.nosqllab2.products;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.Exclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class Product {
    public static final String AVAILABLE = "AVAILABLE";

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

    public Product(ProductRequest request) {
        id = UUID.randomUUID().toString();
        status = AVAILABLE;
        createdAt = new Date();
        apply(request);
    }

    boolean apply(ProductRequest request) {
        boolean changed = !Objects.equals(name, request.name())
                || !Objects.equals(description, request.description())
                || !Objects.equals(price, request.price())
                || !Objects.equals(categoryId, request.categoryId())
                || !Objects.equals(specifications, request.specifications());
        name = request.name();
        description = request.description();
        price = request.price();
        categoryId = request.categoryId();
        specifications = request.specifications();
        return changed;
    }

    @Exclude
    public ProductStatus getActualStatus() {
        if (updatedStatus != null && hasStatusCode(updatedStatus.getCode())) {
            return updatedStatus;
        }
        String code = hasStatusCode(status) ? status : AVAILABLE;
        return new ProductStatus(code, Date.from(Instant.EPOCH), "not updated status");
    }

    public static boolean hasStatusCode(String code) {
        return code != null && !code.isBlank();
    }
}
