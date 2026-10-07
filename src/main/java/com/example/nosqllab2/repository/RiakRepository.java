package com.example.nosqllab2.repository;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.kv.ListKeys;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public abstract class RiakRepository<T> {

    protected final RiakClient client;
    protected final Namespace namespace;
    protected final Class<T> entityClass;

    public RiakRepository(RiakClient client, String bucketName, Class<T> entityClass) {
        this.client = client;
        this.namespace = new Namespace(bucketName);
        this.entityClass = entityClass;
    }

    public RiakRepository(RiakClient client, String bucketType, String bucketName, Class<T> entityClass) {
        this.client = client;
        this.namespace = new Namespace(bucketType, bucketName);
        this.entityClass = entityClass;
    }

    protected abstract String extractId(T entity);

    public void save(T entity) {
        try {
            String key = extractId(entity);
            Location location = new Location(namespace, key);

            StoreValue store = new StoreValue.Builder(entity)
                    .withLocation(location)
                    .build();

            client.execute(store);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Ошибка при сохранении в Riak", e);
        }
    }

    public Optional<T> findById(String id) {
        try {
            Location location = new Location(namespace, id);

            FetchValue fetch = new FetchValue.Builder(location).build();
            FetchValue.Response response = client.execute(fetch);

            if (response.isNotFound()) {
                return Optional.empty();
            }

            T entity = response.getValue(entityClass);
            return Optional.ofNullable(entity);

        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Ошибка при чтении из Riak", e);
        }
    }

    public void delete(String id) {
        try {
            Location location = new Location(namespace, id);
            DeleteValue delete = new DeleteValue.Builder(location).build();
            client.execute(delete);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Ошибка при удалении из Riak", e);
        }
    }

    public List<T> findAll() {
        List<T> result = new ArrayList<>();
        try {
            ListKeys listKeys = new ListKeys.Builder(namespace).build();
            ListKeys.Response response = client.execute(listKeys);

            for (Location location : response) {
                FetchValue fetch = new FetchValue.Builder(location).build();
                FetchValue.Response fetchResponse = client.execute(fetch);
                if (!fetchResponse.isNotFound()) {
                    T entity = fetchResponse.getValue(entityClass);
                    if (entity != null) {
                        result.add(entity);
                    }
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Ошибка при получении списка ключей из Riak", e);
        }
        return result;
    }
}