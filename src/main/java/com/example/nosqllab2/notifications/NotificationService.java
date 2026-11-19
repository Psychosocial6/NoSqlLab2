package com.example.nosqllab2.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.domain.Pageable;
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
    private final NotificationRepository notifications;

    public UUID send(String userId, String channel, String message, DeliveryGateway gateway) {
        UUID notificationId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        notifications.save(new UserNotificationEvent(
                userId, monthBucket(createdAt), createdAt, UUID.randomUUID(),
                notificationId, "CREATED", channel, null, message, null));

        DeliveryResult result = gateway.deliver(userId, channel, message);
        String status = result.delivered() ? "DELIVERED" : "FAILED";
        Instant deliveredAt = Instant.now();
        notifications.saveDelivery(new UserNotificationEvent(
                userId, monthBucket(deliveredAt), deliveredAt, UUID.randomUUID(),
                notificationId, "DELIVERY_RESULT", channel, status, message, result.errorMessage()));

        return notificationId;
    }

    private static LocalDate monthBucket(Instant time) {
        return YearMonth.from(time.atOffset(ZoneOffset.UTC)).atDay(1);
    }

    public HistoryPage findUserEvents(String userId, Instant from, Instant to, int pageSize, HistoryCursor cursor) {
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
        YearMonth month = cursor == null ? newest : cursor.month();
        if (month.isBefore(oldest) || month.isAfter(newest)) {
            throw new IllegalArgumentException("Cursor month is outside the requested period");
        }

        Pageable firstPage = CassandraPageRequest.first(pageSize);
        Pageable pageable = cursor == null ? firstPage : cursor.pageable();
        while (!month.isBefore(oldest)) {
            var slice = notifications.findHistory(userId, month.atDay(1), first, end, pageable);
            if (slice.hasNext()) {
                return new HistoryPage(slice.getContent(), new HistoryCursor(month, slice.nextPageable()));
            }

            month = month.minusMonths(1);
            if (slice.hasContent()) {
                var next = month.isBefore(oldest) ? null : new HistoryCursor(month, firstPage);
                return new HistoryPage(slice.getContent(), next);
            }
            pageable = firstPage;
        }
        return new HistoryPage(List.of(), null);
    }

    private static Instant ceilToMillis(Instant time) {
        Instant truncated = time.truncatedTo(ChronoUnit.MILLIS);
        return time.equals(truncated) ? truncated : truncated.plusMillis(1);
    }

    @FunctionalInterface
    public interface DeliveryGateway {
        DeliveryResult deliver(String userId, String channel, String message);
    }

    public record DeliveryResult(boolean delivered, String errorMessage) {
    }

    public record HistoryCursor(YearMonth month, Pageable pageable) {
    }

    public record HistoryPage(List<UserNotificationEvent> events, HistoryCursor next) {
    }
}
