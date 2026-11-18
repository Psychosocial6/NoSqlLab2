package com.example.nosqllab2.products.cache;

import com.basho.riak.client.api.RiakClient;
import com.example.nosqllab2.repository.RiakCacheRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class ProductCacheRepository extends RiakCacheRepository<ProductCacheEntry> {
    public ProductCacheRepository(RiakClient client,
                                  @Value("${riak.cache-timeout:500ms}") Duration timeout) {
        super(client, "product_catalog_cache", ProductCacheEntry.class, timeout);
    }
}
