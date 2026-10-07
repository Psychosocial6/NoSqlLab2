package com.example.nosqllab2.products;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> getProducts() {
        return productRepository
                .findAll()
                .stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    public ProductResponse getProductById(Long id) {
        return ProductResponse.fromEntity(
                productRepository.findById(id)
                        .orElseThrow(() -> new ProductNotFoundException("product not found"))
        );
    }


    public ProductResponse createProduct(ProductRequest productRequest) {
        ProductEntity productEntity = new ProductEntity(
                productRequest.name(),
                productRequest.description(),
                productRequest.price()
        );

        return ProductResponse.fromEntity(productRepository.save(productEntity));
    }

    @Transactional
    public void deleteProductById(Long id) {
        ProductEntity productEntity = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        productRepository.delete(productEntity);
    }
}
