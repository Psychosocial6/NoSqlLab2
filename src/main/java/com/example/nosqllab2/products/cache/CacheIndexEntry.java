package com.example.nosqllab2.products.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheIndexEntry implements Serializable {
    private String indexKey;
    private Set<String> cacheKeys = new HashSet<>();
}