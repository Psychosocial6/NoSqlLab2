package com.example.nosqllab2.analytics;

import com.example.nosqllab2.exceptions.ApplicationException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

final class ChangeIntervals {
    private static final DateTimeFormatter FORMAT = new DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd_HH")
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .toFormatter(Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final int DEFAULT_HOURS = 7 * 24;
    private static final int MAX_HOURS = 31 * 24;

    private ChangeIntervals() {
    }

    static Range range(String from, String to) {
        LocalDateTime start = parse(from);
        LocalDateTime end = parse(to);
        if (end == null) {
            end = start == null ? currentHour() : start.plusHours(DEFAULT_HOURS - 1L);
        }
        if (start == null) {
            start = end.minusHours(DEFAULT_HOURS - 1L);
        }
        if (start.isAfter(end) || ChronoUnit.HOURS.between(start, end) >= MAX_HOURS) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "Analytics range must be ordered and at most 31 days");
        }
        return new Range(FORMAT.format(start), FORMAT.format(end));
    }

    static String currentKey() {
        return FORMAT.format(currentHour());
    }

    private static LocalDateTime currentHour() {
        return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.HOURS);
    }

    private static LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.strip(), FORMAT);
        } catch (DateTimeParseException exception) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "Use yyyy-MM-dd_HH for UTC analytics intervals");
        }
    }

    record Range(String from, String to) {
    }
}
