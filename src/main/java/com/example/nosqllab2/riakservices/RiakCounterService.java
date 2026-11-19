package com.example.nosqllab2.riakservices;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.datatypes.CounterUpdate;
import com.basho.riak.client.api.commands.datatypes.UpdateCounter;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.crdt.types.RiakCounter;
import com.example.nosqllab2.repository.RiakCalls;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RiakCounterService {
    private static final Location LEAD_COUNTER =
            new Location(new Namespace("counters", "system_sequences"), "leadsservice_id_seq");

    private final RiakClient riakClient;
    private final Duration timeout;

    public RiakCounterService(RiakClient riakClient, @Value("${riak.cache-timeout:500ms}") Duration timeout) {
        this.riakClient = riakClient;
        this.timeout = timeout;
    }

    public long incrementLeadCounter() {
        UpdateCounter command = new UpdateCounter.Builder(LEAD_COUNTER, new CounterUpdate(1))
                .withReturnDatatype(true)
                .build();
        RiakCounter counter = RiakCalls.execute(riakClient, command, timeout).getDatatype();
        if (counter == null) {
            throw new DataAccessResourceFailureException("Riak did not return the updated lead counter");
        }
        return counter.view();
    }
}
