package com.example.nosqllab2.favorites;

import com.example.nosqllab2.products.ProductEntity;
import com.example.nosqllab2.products.ProductNotFoundException;
import com.example.nosqllab2.products.ProductResponse;
import com.example.nosqllab2.products.ProductRepository;
import com.example.nosqllab2.operations.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@RequiredArgsConstructor
@Service
public class FavoriteService {

    private final UserFavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final OperationService operationService;

    public List<ProductResponse> getUsersFavorites(Long userId) {
        return favoriteRepository.findProductsByUserId(userId)
                .stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void addFavorite(Long userId, Long productId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        UserFavoriteId id = new UserFavoriteId(userId, productId);
        if (!favoriteRepository.existsById(id)) {
            favoriteRepository.save(new UserFavoriteEntity(userId, product));
        }

        operationService.logOperation(userId, String.format("User (id=%d) added product (id=%d) to favs", userId, productId));
    }

    @Transactional
    public void deleteFavorite(Long userId, Long productId) {
        favoriteRepository.deleteById(new UserFavoriteId(userId, productId));
        operationService.logOperation(
                userId,
                String.format("User (id=%d) deleted product (id=%d) from favs", userId, productId)
        );
    }
}