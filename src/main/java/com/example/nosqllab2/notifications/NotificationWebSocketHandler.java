package com.example.nosqllab2.notifications;

import com.example.nosqllab2.users.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    private final WebSocketDeliveryService delivery;
    private final ObjectMapper json;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        if (!(session.getPrincipal() instanceof Authentication authentication)
                || !(authentication.getPrincipal() instanceof UserPrincipal user)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        session.setTextMessageSizeLimit(1024);
        delivery.connect(session, user.id());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        if (!(session.getPrincipal() instanceof Authentication authentication)
                || !(authentication.getPrincipal() instanceof UserPrincipal user)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        Ack ack;
        try {
            ack = json.readValue(message.getPayload(), Ack.class);
        } catch (JacksonException exception) {
            delivery.reply(session, "INVALID_ACK", null);
            return;
        }
        if (ack == null || !"ACK".equals(ack.type()) || ack.attemptId() == null) {
            delivery.reply(session, "INVALID_ACK", null);
            return;
        }
        try {
            boolean accepted = delivery.acknowledge(user.id(), ack.attemptId());
            delivery.reply(session, accepted ? "ACKNOWLEDGED" : "INVALID_ACK", ack.attemptId());
        } catch (DataAccessException exception) {
            delivery.reply(session, "ACK_RETRY", ack.attemptId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        delivery.disconnect(session);
    }

    private record Ack(String type, UUID attemptId) {}
}
