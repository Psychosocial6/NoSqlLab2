package com.example.nosqllab2.products.cache;


import com.basho.riak.client.api.RiakClient;
import com.example.nosqllab2.repository.RiakRepository;
import org.springframework.stereotype.Repository;


@Repository
public class ProductCacheRepository extends RiakRepository<ProductCacheEntry> {

    public ProductCacheRepository(RiakClient client) {
        super(client, "product_cache", ProductCacheEntry.class);
    }

    @Override
    protected String extractId(ProductCacheEntry entity) {
        return entity.getCacheKey();
    }
}