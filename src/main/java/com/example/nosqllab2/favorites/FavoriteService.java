package com.example.nosqllab2.favorites;

import com.example.nosqllab2.products.Product;
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

    public List<ProductResponse> getUsersFavorites(String userId) {
        return favoriteRepository.findByUserId(userId)
                .collectList()
                .block()
                .stream()
                .map(favorite -> ProductResponse.fromEntity(favorite.getProduct()))
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
            favoriteRepository.save(new UserFavorite(userId, product)).block();
        }

        operationService.logOperation(userId, String.format("User (id=%s) added product (id=%s) to favs", userId, productId));
    }

    @Transactional
    public void deleteFavorite(String userId, String productId) {
        String id = userId + "_" + productId;
        favoriteRepository.deleteById(id).block();
        operationService.logOperation(
                userId,
                String.format("User (id=%s) deleted product (id=%s) from favs", userId, productId)
        );
    }
}