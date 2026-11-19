package com.example.nosqllab2.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final int MAX_MONTHS_PER_PAGE = 12;

    private final NotificationRepository notifications;

    public UUID create(String userId, String channel, String message) {
        UUID notificationId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        notifications.save(new UserNotificationEvent(
                userId, monthBucket(createdAt), createdAt, UUID.randomUUID(),
                notificationId, "CREATED", channel, null, message, null));
        return notificationId;
    }

    static LocalDate monthBucket(Instant time) {
        return YearMonth.from(time.atOffset(ZoneOffset.UTC)).atDay(1);
    }

    public HistoryPage findUserEvents(String userId, Instant from, Instant to, int pageSize,
                                     Sort.Direction sort, HistoryCursor cursor) {
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be earlier than to");
        }
        Instant first = ceilToMillis(from);
        Instant end = ceilToMillis(to);
        if (!first.isBefore(end)) {
            return new HistoryPage(List.of(), null);
        }

        YearMonth oldest = YearMonth.from(first.atOffset(ZoneOffset.UTC));
        YearMonth newest = YearMonth.from(end.minusMillis(1).atOffset(ZoneOffset.UTC));
        boolean ascending = sort.isAscending();
        YearMonth firstMonth = ascending ? oldest : newest;
        YearMonth lastMonth = ascending ? newest : oldest;
        YearMonth month = cursor == null ? firstMonth : cursor.month();
        if (month.isBefore(oldest) || month.isAfter(newest)) {
            throw new IllegalArgumentException("Cursor month is outside the requested period");
        }

        Pageable firstPage = CassandraPageRequest.first(pageSize);
        Pageable pageable = cursor == null ? firstPage : cursor.pageable();
        for (int scanned = 0; scanned < MAX_MONTHS_PER_PAGE; scanned++) {
            var slice = ascending
                    ? notifications.findHistoryAscending(userId, month.atDay(1), first, end, pageable)
                    : notifications.findHistory(userId, month.atDay(1), first, end, pageable);
            if (slice.hasNext()) {
                return new HistoryPage(slice.getContent(), new HistoryCursor(month, slice.nextPageable()));
            }

            if (month.equals(lastMonth)) {
                return new HistoryPage(slice.getContent(), null);
            }
            month = month.plusMonths(ascending ? 1 : -1);
            if (slice.hasContent()) {
                return new HistoryPage(slice.getContent(), new HistoryCursor(month, firstPage));
            }
            pageable = firstPage;
        }
        return new HistoryPage(List.of(), new HistoryCursor(month, firstPage));
    }

    private static Instant ceilToMillis(Instant time) {
        Instant truncated = time.truncatedTo(ChronoUnit.MILLIS);
        return time.equals(truncated) ? truncated : truncated.plusMillis(1);
    }

    public record HistoryCursor(YearMonth month, Pageable pageable) {
    }

    public record HistoryPage(List<UserNotificationEvent> events, HistoryCursor next) {
    }
}
