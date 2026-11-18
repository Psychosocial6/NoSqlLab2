package com.example.nosqllab2.storage;

import com.example.nosqllab2.exceptions.ApplicationException;
import com.google.cloud.firestore.DocumentSnapshot;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

public record DocumentPage<T>(List<T> items, String nextCursor) {
    public static <T> DocumentPage<T> from(
            List<? extends DocumentSnapshot> documents, int size, Function<? super DocumentSnapshot, T> mapper) {
        int end = Math.min(size, documents.size());
        String next = documents.size() > size ? encode(documents.get(end - 1).getId()) : null;
        List<T> items = documents.subList(0, end).stream().map(mapper).toList();
        return new DocumentPage<>(items, next);
    }

    public ResponseEntity<List<T>> response() {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (nextCursor != null) {
            response.header("X-Next-Cursor", nextCursor);
        }
        return response.body(items);
    }

    public static String decodeCursor(String cursor) {
        if (cursor == null) {
            return null;
        }
        if (cursor.length() <= 2000) {
            try {
                byte[] decoded = Base64.getUrlDecoder().decode(cursor);
                String id = new String(decoded, StandardCharsets.UTF_8);
                if (!id.isBlank() && !id.contains("/") && !id.equals(".") && !id.equals("..")
                        && encode(id).equals(cursor)) {
                    return id;
                }
            } catch (IllegalArgumentException ignored) {
                // Invalid cursors share the same API error.
            }
        }
        throw new ApplicationException(HttpStatus.BAD_REQUEST, "Invalid pagination cursor");
    }

    private static String encode(String id) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(id.getBytes(StandardCharsets.UTF_8));
    }
}
