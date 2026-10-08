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
@Document(collectionName = "user_favorites")
public class UserFavorite {
    @DocumentId
    private String id;

    private String userId;
    private String productId;

    public UserFavorite(String userId, String productId) {
        this.id = userId + "_" + productId;
        this.userId = userId;
        this.productId = productId;
    }
}