package com.example.nosqllab2.notifications;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.SessionLimitExceededException;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class WebSocketDeliveryService {
    private static final String CHANNEL = "WEBSOCKET";
    private static final Duration ACK_RETENTION = Duration.ofMinutes(10);

    private final NotificationService notifications;
    private final NotificationRepository repository;
    private final ObjectMapper json;
    private final Duration ackTimeout;
    private final ConcurrentMap<String, Connection> connections = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Attempt> attempts = new ConcurrentHashMap<>();

    public WebSocketDeliveryService(NotificationService notifications, NotificationRepository repository,
                                    ObjectMapper json, @Value("${notifications.websocket.ack-timeout:PT10S}") Duration ackTimeout) {
        this.notifications = notifications;
        this.repository = repository;
        this.json = json;
        if (ackTimeout.isNegative() || ackTimeout.isZero()) {
            throw new IllegalArgumentException("WebSocket ACK timeout must be positive");
        }
        this.ackTimeout = ackTimeout;
    }

    void connect(WebSocketSession session, String userId) {
        connections.put(session.getId(), new Connection(userId,
                new ConcurrentWebSocketSessionDecorator(session, 5_000, 64 * 1024)));
    }

    void disconnect(WebSocketSession session) {
        connections.remove(session.getId());
    }

    @EventListener
    void closeEndedSession(HttpSessionDestroyedEvent event) {
        connections.forEach((id, connection) -> {
            var socket = connection.socket();
            if (event.getId().equals(socket.getAttributes().get(HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME))
                    && connections.remove(id, connection)) {
                try {
                    socket.close(CloseStatus.POLICY_VIOLATION);
                } catch (IOException | IllegalStateException exception) {
                    log.debug("Could not close WebSocket session {}", id, exception);
                }
            }
        });
    }

    public void send(String userId, String message) {
        UUID notificationId = notifications.create(userId, CHANNEL, message);
        UUID attemptId = UUID.randomUUID();
        var attempt = new Attempt(notificationId, userId, message, Instant.now().plus(ackTimeout));
        var frame = new TextMessage(json.writeValueAsString(
                new NotificationFrame("NOTIFICATION", notificationId, attemptId, message)));
        attempts.put(attemptId, attempt);

        boolean connected = false;
        boolean sent = false;
        for (var connection : connections.values()) {
            if (connection.userId().equals(userId) && connection.socket().isOpen()) {
                connected = true;
                sent |= sendFrame(connection.socket(), frame);
            }
        }
        synchronized (attempt) {
            attempt.sending = false;
            if (!sent && attempt.ack == null) {
                attempt.failed = true;
                attempt.result = attempt.event(UUID.randomUUID(), Instant.now(), "FAILED",
                        connected ? "SEND_ERROR" : "NO_ACTIVE_SESSION");
                flush(attempt);
            }
        }
    }

    boolean acknowledge(String userId, UUID attemptId) {
        var attempt = attempts.get(attemptId);
        if (attempt == null || !attempt.userId.equals(userId)) {
            return false;
        }
        synchronized (attempt) {
            Instant now = Instant.now();
            if (attempt.failed || !now.isBefore(attempt.forgetAt)) {
                return false;
            }
            noteTimeout(attempt, now);
            if (attempt.ack == null) {
                attempt.ack = attempt.event(attemptId, now, "DELIVERED", null);
            }
            flush(attempt);
            return true;
        }
    }

    void reply(WebSocketSession session, String type, UUID attemptId) {
        var connection = connections.get(session.getId());
        if (connection != null) {
            sendFrame(connection.socket(), new TextMessage(json.writeValueAsString(new AckFrame(type, attemptId))));
        }
    }

    @Scheduled(fixedDelay = 1000)
    void checkAttempts() {
        Instant now = Instant.now();
        attempts.forEach((id, attempt) -> {
            synchronized (attempt) {
                try {
                    noteTimeout(attempt, now);
                    flush(attempt);
                } catch (DataAccessException exception) {
                    log.warn("Could not persist WebSocket delivery attempt {}", id, exception);
                }
                if (!now.isBefore(attempt.forgetAt)) {
                    attempts.remove(id, attempt);
                }
            }
        });
    }

    private static void noteTimeout(Attempt attempt, Instant now) {
        if (!attempt.sending && attempt.result == null && attempt.ack == null && !now.isBefore(attempt.deadline)) {
            attempt.result = attempt.event(UUID.randomUUID(), attempt.deadline, "UNCONFIRMED", "ACK_TIMEOUT");
        }
    }

    private void flush(Attempt attempt) {
        if (attempt.result != null && !attempt.resultSaved) {
            repository.saveDelivery(attempt.result);
            attempt.resultSaved = true;
        }
        if (attempt.ack != null && !attempt.ackSaved) {
            repository.saveDelivery(attempt.ack);
            attempt.ackSaved = true;
        }
    }

    private boolean sendFrame(WebSocketSession socket, TextMessage frame) {
        try {
            socket.sendMessage(frame);
            return true;
        } catch (IOException | IllegalStateException | SessionLimitExceededException exception) {
            log.debug("Could not write to WebSocket session {}", socket.getId(), exception);
            return false;
        }
    }

    private record Connection(String userId, WebSocketSession socket) {}
    private record NotificationFrame(String type, UUID notificationId, UUID attemptId, String message) {}
    private record AckFrame(String type, UUID attemptId) {}

    private static final class Attempt {
        private final UUID notificationId;
        private final String userId;
        private final String message;
        private final Instant deadline;
        private final Instant forgetAt;
        private UserNotificationEvent result;
        private UserNotificationEvent ack;
        private boolean sending = true;
        private boolean resultSaved;
        private boolean ackSaved;
        private boolean failed;

        private Attempt(UUID notificationId, String userId, String message, Instant deadline) {
            this.notificationId = notificationId;
            this.userId = userId;
            this.message = message;
            this.deadline = deadline;
            this.forgetAt = deadline.plus(ACK_RETENTION);
        }

        private UserNotificationEvent event(UUID eventId, Instant time, String status, String error) {
            return new UserNotificationEvent(userId, NotificationService.monthBucket(time), time, eventId,
                    notificationId, "DELIVERY_RESULT", CHANNEL, status, message, error);
        }
    }
}
