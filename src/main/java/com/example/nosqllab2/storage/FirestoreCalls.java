package com.example.nosqllab2.storage;

import com.example.nosqllab2.exceptions.ApplicationException;
import com.google.api.core.ApiFuture;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.concurrent.ExecutionException;

public final class FirestoreCalls {
    private FirestoreCalls() {
    }

    public static <T> T await(ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DataAccessResourceFailureException("Firestore request interrupted", exception);
        } catch (ExecutionException exception) {
            for (Throwable cause = exception.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof ApplicationException applicationError) {
                    throw applicationError;
                }
            }
            throw new DataAccessResourceFailureException("Firestore request failed", exception.getCause());
        }
    }
}
