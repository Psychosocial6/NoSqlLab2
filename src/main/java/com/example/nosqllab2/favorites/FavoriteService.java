package com.example.nosqllab2.favorites;

import com.example.nosqllab2.products.ProductRepository;
import com.example.nosqllab2.products.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class FavoriteService {
    private final UserFavoriteRepository favorites;
    private final ProductRepository products;

    public List<ProductResponse> getUsersFavorites(String userId) {
        return products.getAll(favorites.findProductIds(userId));
    }

    public boolean addFavorite(String userId, String productId) {
        return favorites.add(userId, productId);
    }

    public void deleteFavorite(String userId, String productId) {
        favorites.delete(userId, productId);
    }
}
