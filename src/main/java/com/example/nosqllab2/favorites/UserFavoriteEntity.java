package com.example.nosqllab2.favorites;

import com.example.nosqllab2.products.ProductEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_favorites")
@Getter
@Setter
@NoArgsConstructor
public class UserFavoriteEntity {

    @EmbeddedId
    private UserFavoriteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    public UserFavoriteEntity(Long userId, ProductEntity product) {
        this.id = new UserFavoriteId(userId, product.getId());
        this.product = product;
    }
}