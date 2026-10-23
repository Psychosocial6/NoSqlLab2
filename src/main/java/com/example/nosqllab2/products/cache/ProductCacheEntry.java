package com.example.nosqllab2.products.cache;

import com.example.nosqllab2.products.ProductResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCacheEntry implements Serializable {
    private String cacheKey;
    private String categoryId;
    private List<ProductResponse> products;
    private long expiresAt;
}