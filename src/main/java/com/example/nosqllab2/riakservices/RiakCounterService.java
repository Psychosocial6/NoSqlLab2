package com.example.nosqllab2.riakservices;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.datatypes.CounterUpdate;
import com.basho.riak.client.api.commands.datatypes.UpdateCounter;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.example.nosqllab2.repository.RiakCalls;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RiakCounterService {
    private static final Location LEADS_CREATED =
            new Location(new Namespace("counters", "system_sequences"), "leads_created");

    private final RiakClient riakClient;
    private final Duration timeout;

    public RiakCounterService(RiakClient riakClient, @Value("${riak.cache-timeout:500ms}") Duration timeout) {
        this.riakClient = riakClient;
        this.timeout = timeout;
    }

    public void recordLeadCreated() {
        UpdateCounter command = new UpdateCounter.Builder(LEADS_CREATED, new CounterUpdate(1)).build();
        RiakCalls.execute(riakClient, command, timeout);
    }
}
