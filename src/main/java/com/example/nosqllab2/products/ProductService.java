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
                .collectList()
                .block()
                .stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id).block();
        if (product == null) {
            throw new ProductNotFoundException("Product not found");
        }
        return ProductResponse.fromEntity(product);
    }


    public ProductResponse createProduct(ProductRequest productRequest) {
        Product product = new Product(
                productRequest.name(),
                productRequest.description(),
                productRequest.price()
        );
        Product saved = productRepository.save(product).block();
        return ProductResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteProductById(String id) {
        Product product = productRepository.findById(id).block();
        if (product == null) {
            throw new ProductNotFoundException("Product not found");
        }

        productRepository.delete(product).block();
    }
}
