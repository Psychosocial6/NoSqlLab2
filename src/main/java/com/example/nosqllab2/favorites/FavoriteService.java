package com.example.nosqllab2.favorites;

import com.example.nosqllab2.exceptions.ProductNotFoundException;
import com.example.nosqllab2.products.ProductResponse;
import com.example.nosqllab2.products.ProductRepository;
import com.example.nosqllab2.operations.OperationService;
import com.example.nosqllab2.products.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
@Service
public class FavoriteService {

    private final UserFavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final OperationService operationService;

    public List<ProductResponse> getUsersFavorites(String userId) {
        return favoriteRepository.findByUserId(userId)
                .flatMap(fav -> productRepository.findById(fav.getProductId()))
                .map(ProductResponse::fromEntity)
                .collectList()
                .block();
    }

    public void addFavorite(String userId, String productId) {
        String id = userId + "_" + productId;
        productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new ProductNotFoundException("product not found")))
                .flatMap(product -> {
                    ProductStatus currentStatus = product.getActualStatus();
                    if (!"AVAILABLE".equalsIgnoreCase(currentStatus.getCode())) {
                        return Mono.error(new IllegalStateException(
                                "cannot add product to fav, status: " + currentStatus.getCode()
                        ));
                    }

                    return favoriteRepository.existsById(id);
                })
                .flatMap(exists -> {
                    if (!exists) {
                        return favoriteRepository.save(new UserFavorite(userId, productId));
                    }
                    return Mono.empty();
                })
                .block();

        operationService.logOperation(
                userId,
                String.format("User (id=%s) added product (id=%s) to favs", userId, productId)
        );
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