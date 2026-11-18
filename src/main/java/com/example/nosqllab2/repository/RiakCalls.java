package com.example.nosqllab2.repository;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.RiakCommand;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class RiakCalls {
    private RiakCalls() {
    }

    public static <T> T execute(RiakClient client, RiakCommand<T, ?> command, Duration timeout) {
        try {
            return client.execute(command, timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DataAccessResourceFailureException("Riak request interrupted", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new DataAccessResourceFailureException("Riak request failed", exception);
        }
    }
}
