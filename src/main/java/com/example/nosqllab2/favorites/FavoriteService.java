package com.example.nosqllab2.favorites;

import com.example.nosqllab2.products.Product;
import com.example.nosqllab2.exceptions.ProductNotFoundException;
import com.example.nosqllab2.products.ProductResponse;
import com.example.nosqllab2.products.ProductRepository;
import com.example.nosqllab2.operations.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class FavoriteService {

    private final UserFavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final OperationService operationService;

    public List<ProductResponse> getUsersFavorites(String userId) {
        List<UserFavorite> favorites = favoriteRepository.findByUserId(userId)
                .collectList()
                .block();

        if (favorites == null || favorites.isEmpty()) {
            return List.of();
        }

        return favorites.stream()
                .map(fav -> productRepository.findById(fav.getProductId()).block())
                .filter(Objects::nonNull)
                .map(ProductResponse::fromEntity)
                .toList();
    }

    public void addFavorite(String userId, String productId) {
        Product product = productRepository.findById(productId).block();
        if (product == null) {
            throw new ProductNotFoundException("Product not found");
        }

        String id = userId + "_" + productId;
        Boolean exists = favoriteRepository.existsById(id).block();

        if (Boolean.FALSE.equals(exists)) {
            favoriteRepository.save(new UserFavorite(userId, productId)).block();
        }

        operationService.logOperation(userId, String.format("User (id=%s) added product (id=%s) to favs", userId, productId));
    }

    public void deleteFavorite(String userId, String productId) {
        String id = userId + "_" + productId;
        favoriteRepository.deleteById(id).block();
        operationService.logOperation(
                userId,
                String.format("User (id=%s) deleted product (id=%s) from favs", userId, productId)
        );
    }
}