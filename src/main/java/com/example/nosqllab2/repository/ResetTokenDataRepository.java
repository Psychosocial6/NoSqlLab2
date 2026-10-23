package com.example.nosqllab2.repository;

import com.basho.riak.client.api.RiakClient;
import com.example.nosqllab2.models.ResetTokenData;
import org.springframework.stereotype.Repository;

@Repository
public class ResetTokenDataRepository extends RiakRepository<ResetTokenData> {

    public ResetTokenDataRepository(RiakClient client) {
        super(client, "expiration_bucket", "reset-tokens", ResetTokenData.class);
    }

    @Override
    protected String extractId(ResetTokenData entity) {
        return entity.getUsername();
    }
}