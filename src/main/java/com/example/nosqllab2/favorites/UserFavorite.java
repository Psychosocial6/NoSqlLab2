package com.example.nosqllab2.favorites;

import com.example.nosqllab2.products.Product;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Document
public class UserFavorite {
    @DocumentId
    private String id;

    private String userId;
    private Product product;

    public UserFavorite(String userId, Product product) {
        this.id = userId + "_" + product.getId();
        this.userId = userId;
        this.product = product;
    }
}