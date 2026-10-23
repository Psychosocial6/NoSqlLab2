package com.example.nosqllab2.products.cache;

import com.basho.riak.client.api.RiakClient;
import com.example.nosqllab2.repository.RiakRepository;
import org.springframework.stereotype.Repository;

@Repository
public class CacheIndexRepository extends RiakRepository<CacheIndexEntry> {

    public CacheIndexRepository(RiakClient client) {
        super(client, "product_cache", CacheIndexEntry.class);
    }

    @Override
    protected String extractId(CacheIndexEntry entity) {
        return entity.getIndexKey();
    }
}