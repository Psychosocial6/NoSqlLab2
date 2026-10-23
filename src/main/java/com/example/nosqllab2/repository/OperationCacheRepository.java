package com.example.nosqllab2.repository;

import com.basho.riak.client.api.RiakClient;
import com.example.nosqllab2.models.OperationCache;
import org.springframework.stereotype.Repository;

@Repository
public class OperationCacheRepository extends RiakRepository<OperationCache> {
    public OperationCacheRepository(RiakClient client) {
        super(client, "operation_cache", OperationCache.class);
    }

    @Override
    protected String extractId(OperationCache entity) {
        return entity.userId();
    }
}
