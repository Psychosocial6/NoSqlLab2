package com.example.nosqllab2.repository;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.cap.UnresolvedConflictException;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.convert.ConversionException;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Duration;
import java.util.Optional;

public abstract class RiakRepository<T> {
    private final RiakClient client;
    private final Namespace namespace;
    private final Class<T> entityClass;
    private final Duration timeout;

    protected RiakRepository(RiakClient client, Namespace namespace, Class<T> entityClass, Duration timeout) {
        this.client = client;
        this.namespace = namespace;
        this.entityClass = entityClass;
        this.timeout = timeout;
    }

    protected abstract String extractId(T entity);

    public void save(T entity) {
        try {
            StoreValue command = new StoreValue.Builder(entity).withLocation(location(extractId(entity))).build();
            RiakCalls.execute(client, command, timeout);
        } catch (ConversionException exception) {
            throw new DataAccessResourceFailureException("Could not encode Riak value", exception);
        }
    }

    public Optional<T> findById(String id) {
        FetchValue.Response response = RiakCalls.execute(client, new FetchValue.Builder(location(id)).build(), timeout);
        if (response.isNotFound()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(response.getValue(entityClass));
        } catch (ConversionException | UnresolvedConflictException exception) {
            throw new DataAccessResourceFailureException("Could not read a unique Riak value", exception);
        }
    }

    public void delete(String id) {
        RiakCalls.execute(client, new DeleteValue.Builder(location(id)).build(), timeout);
    }

    private Location location(String id) {
        return new Location(namespace, id);
    }
}
