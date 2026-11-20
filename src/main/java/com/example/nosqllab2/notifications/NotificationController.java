package com.example.nosqllab2.notifications;

import com.example.nosqllab2.users.UserPrincipal;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.WebUtils;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationController {
    private static final String CURSORS = NotificationController.class.getName() + ".cursors";
    private static final int MAX_CURSORS = 32;
    private static final Instant MIN_TIMESTAMP = Instant.ofEpochMilli(Long.MIN_VALUE);
    private static final Instant MAX_TIMESTAMP = Instant.ofEpochMilli(Long.MAX_VALUE);

    private final NotificationService notifications;
    private final NotificationDeliveryAttemptRepository attempts;
    private final DeliveryByChannelStatusRepository deliveries;

    @GetMapping("/notifications")
    public PageResponse<UserNotificationEvent> history(@AuthenticationPrincipal UserPrincipal principal,
                                   @RequestParam Instant from, @RequestParam Instant to,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(defaultValue = "DESC") Sort.Direction sort,
                                   @RequestParam(required = false) UUID cursor,
                                   HttpSession session) {
        if (!from.isBefore(to) || from.isBefore(MIN_TIMESTAMP) || to.isAfter(MAX_TIMESTAMP)
                || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid period or page size");
        }
        var query = new HistoryQuery(principal.id(), from, to, size, sort);
        var position = cursor == null ? null : resume(session, cursor, query, NotificationService.HistoryCursor.class);
        var page = notifications.findUserEvents(query.userId(), from, to, size, sort, position);
        UUID next = page.next() == null ? null : remember(session, query, page.next());
        return new PageResponse<>(page.events(), next);
    }

    @GetMapping("/notifications/{notificationId}/attempts")
    public PageResponse<NotificationDeliveryAttempt> attempts(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID notificationId,
            @RequestParam YearMonth month, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID cursor, HttpSession session) {
        var period = monthPeriod(month);
        var query = new AttemptsQuery(principal.id(), notificationId, month, size);
        Pageable pageable = pageRequest(session, cursor, query, size);
        boolean owned = attempts.findLatestAttempt(notificationId, month.atDay(1))
                .filter(row -> principal.id().equals(row.userId()))
                .isPresent();
        if (!owned) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification attempts not found");
        }
        var page = attempts.findAttempts(notificationId, month.atDay(1), period.from(), period.to(), pageable);
        if (page.getContent().stream().anyMatch(row -> !principal.id().equals(row.userId()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification attempts not found");
        }
        return pageResponse(session, query, page);
    }

    @GetMapping("/admin/notifications/deliveries")
    public PageResponse<DeliveryByChannelStatus> deliveries(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String channel, @RequestParam String status, @RequestParam YearMonth month,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) UUID cursor,
            HttpSession session) {
        if (channel.isBlank() || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel and status are required");
        }
        var period = monthPeriod(month);
        var query = new DeliveriesQuery(principal.id(), channel, status, month, size);
        Pageable pageable = pageRequest(session, cursor, query, size);
        var page = deliveries.findDeliveries(channel, status, month.atDay(1), period.from(), period.to(), pageable);
        return pageResponse(session, query, page);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Void> invalidQueryParameter() {
        return ResponseEntity.badRequest().build();
    }

    private static MonthPeriod monthPeriod(YearMonth month) {
        try {
            Math.toIntExact(month.atDay(1).toEpochDay());
            var from = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            var to = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            from.toEpochMilli();
            to.toEpochMilli();
            return new MonthPeriod(from, to);
        } catch (DateTimeException | ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Month is out of range", exception);
        }
    }

    private static Pageable pageRequest(HttpSession session, UUID cursor, Object query, int size) {
        if (size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page size");
        }
        return cursor == null ? CassandraPageRequest.first(size) : resume(session, cursor, query, Pageable.class);
    }

    private static <T> PageResponse<T> pageResponse(HttpSession session, Object query, Slice<T> page) {
        UUID next = page.hasNext() ? remember(session, query, page.nextPageable()) : null;
        return new PageResponse<>(page.getContent(), next);
    }

    private static <T> T resume(HttpSession session, UUID token, Object query, Class<T> positionType) {
        synchronized (WebUtils.getSessionMutex(session)) {
            var store = (CursorStore) session.getAttribute(CURSORS);
            var saved = store == null ? null : store.positions.get(token);
            if (saved == null || !saved.query().equals(query)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid notification cursor");
            }
            return positionType.cast(saved.position());
        }
    }

    private static UUID remember(HttpSession session, Object query, Object position) {
        synchronized (WebUtils.getSessionMutex(session)) {
            var store = (CursorStore) session.getAttribute(CURSORS);
            if (store == null) {
                store = new CursorStore();
                session.setAttribute(CURSORS, store);
            }
            if (store.positions.size() == MAX_CURSORS) {
                store.positions.pollFirstEntry();
            }
            UUID token = UUID.randomUUID();
            store.positions.put(token, new SavedCursor(query, position));
            return token;
        }
    }

    private static final class CursorStore {
        private final LinkedHashMap<UUID, SavedCursor> positions = new LinkedHashMap<>();
    }

    private record HistoryQuery(String userId, Instant from, Instant to, int size, Sort.Direction sort) {}
    private record AttemptsQuery(String userId, UUID notificationId, YearMonth month, int size) {}
    private record DeliveriesQuery(String userId, String channel, String status, YearMonth month, int size) {}
    private record MonthPeriod(Instant from, Instant to) {}
    private record SavedCursor(Object query, Object position) {}
    public record PageResponse<T>(List<T> events, UUID nextCursor) {}
}
