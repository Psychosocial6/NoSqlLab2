package com.example.nosqllab2.products;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RequestMapping("/api/product")
@RestController
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String charKey,
            @RequestParam(required = false) String charVal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        List<ProductResponse> products = productService.getProducts(categoryId, charKey, charVal, pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable @NotNull(message = "id required") String id) {
        ProductResponse productResponse = productService.getProductById(id);
        return ResponseEntity.ok(productResponse);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest productRequest) {
        ProductResponse productResponse = productService.createProduct(productRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(productResponse);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductById(@PathVariable @NotNull(message = "id required") String id) {
        productService.deleteProductById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable @NotNull(message = "id required") String id,
            @Valid @RequestBody ProductRequest productRequest) {
        ProductResponse updated = productService.updateProduct(id, productRequest);
        return ResponseEntity.ok(updated);
    }
}
