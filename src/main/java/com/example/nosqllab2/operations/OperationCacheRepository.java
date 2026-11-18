package com.example.nosqllab2.operations;

import com.basho.riak.client.api.RiakClient;
import com.example.nosqllab2.repository.RiakCacheRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class OperationCacheRepository extends RiakCacheRepository<OperationCache> {
    public OperationCacheRepository(RiakClient client, @Value("${riak.cache-timeout:500ms}") Duration timeout) {
        super(client, "operation_history_cache", OperationCache.class, timeout);
    }
}
