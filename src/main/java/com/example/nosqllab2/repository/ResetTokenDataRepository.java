package com.example.nosqllab2.repository;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.core.query.Namespace;
import com.example.nosqllab2.models.ResetTokenData;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class ResetTokenDataRepository extends RiakRepository<ResetTokenData> {
    public ResetTokenDataRepository(RiakClient client) {
        super(client, new Namespace("expiration_bucket", "reset-tokens"), ResetTokenData.class, Duration.ofSeconds(5));
    }

    @Override
    protected String extractId(ResetTokenData entity) {
        return entity.getUsername();
    }
}
