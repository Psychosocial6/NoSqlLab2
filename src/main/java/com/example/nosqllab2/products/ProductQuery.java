package com.example.nosqllab2.products;

import com.example.nosqllab2.exceptions.ApplicationException;
import com.example.nosqllab2.storage.DocumentPage;
import com.example.nosqllab2.storage.StorageKey;
import com.google.cloud.firestore.Query;
import org.springframework.http.HttpStatus;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ProductQuery(
        String categoryId, String specField, String specValue, String status,
        String sortBy, Query.Direction direction, int size, int offset, String afterId
) {
    private static final Set<String> SPEC_FIELDS = Set.of("brand", "color", "material");
    private static final Set<String> SORT_FIELDS = Set.of("name", "price", "createdAt");

    public static ProductQuery of(String category, String field, String value, String status,
                                  int page, int size, String sortBy, String direction, String after) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > 1000 || (after != null && page != 0)) {
            throw invalid("Use page >= 0, size 1–100 and a cursor for deeper pages");
        }
        if (sortBy == null || !SORT_FIELDS.contains(sortBy)) {
            throw invalid("sortBy must be name, price or createdAt");
        }
        Query.Direction order = switch (direction == null ? "" : direction.toLowerCase(Locale.ROOT)) {
            case "asc" -> Query.Direction.ASCENDING;
            case "desc" -> Query.Direction.DESCENDING;
            default -> throw invalid("direction must be asc or desc");
        };
        field = optionalText(field, 32);
        value = optionalText(value, 256);
        if ((field == null) != (value == null) || (field != null && !SPEC_FIELDS.contains(field))) {
            throw invalid("Specify brand, color or material together with specVal");
        }
        return new ProductQuery(optionalText(category, 256), field, value, optionalText(status, 64),
                sortBy, order, size, page * size, DocumentPage.decodeCursor(after));
    }

    public String cacheKey() {
        String parameters = Stream.of(categoryId, specField, specValue, status, sortBy,
                        direction.name(), Integer.toString(size), Integer.toString(offset), afterId)
                .map(value -> value == null ? "-:" : value.length() + ":" + value)
                .collect(Collectors.joining());
        return StorageKey.hash(parameters);
    }

    private static String optionalText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.strip();
        if (cleaned.length() > maxLength) {
            throw invalid("Catalog filter value is too long");
        }
        return cleaned;
    }

    private static ApplicationException invalid(String message) {
        return new ApplicationException(HttpStatus.BAD_REQUEST, message);
    }
}
