package com.example.nosqllab2.riakservices;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.datatypes.CounterUpdate;
import com.basho.riak.client.api.commands.datatypes.FetchCounter;
import com.basho.riak.client.api.commands.datatypes.UpdateCounter;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.crdt.types.RiakCounter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
@Service
public class RiakCounterService {

    private final RiakClient riakClient;

    private static final String BUCKET_TYPE = "counters";
    private static final String SYSTEM_BUCKET = "system_sequences";

    public long generateNextId(Class<?> entityClass) throws ExecutionException, InterruptedException {
        String counterKey = entityClass.getSimpleName().toLowerCase() + "_id_seq";

        return incrementAndGet(counterKey, 1);
    }

    public long incrementAndGet(String counterKey, long amount) throws ExecutionException, InterruptedException {
        Location location = new Location(new Namespace(BUCKET_TYPE, SYSTEM_BUCKET), counterKey);

        CounterUpdate counterUpdate = new CounterUpdate(amount);
        UpdateCounter update = new UpdateCounter.Builder(location, counterUpdate).build();
        riakClient.execute(update);

        FetchCounter fetch = new FetchCounter.Builder(location).build();
        FetchCounter.Response response = riakClient.execute(fetch);

        RiakCounter counter = response.getDatatype();
        return counter != null ? counter.view() : 0L;
    }


}

