package com.example.nosqllab2.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.data.cassandra.core.CassandraTemplate;

interface NotificationDeliveryWrites {
    void saveDelivery(UserNotificationEvent event);
}

@RequiredArgsConstructor
class NotificationDeliveryWritesImpl implements NotificationDeliveryWrites {
    private final CassandraTemplate cassandra;

    @Override
    public void saveDelivery(UserNotificationEvent event) {
        var attempt = new NotificationDeliveryAttempt(
                event.notificationId(), event.monthBucket(), event.eventTime(), event.eventId(),
                event.userId(), event.channel(), event.deliveryStatus(), event.errorMessage());
        var delivery = new DeliveryByChannelStatus(
                event.channel(), event.deliveryStatus(), event.monthBucket(), event.eventTime(), event.eventId(),
                event.notificationId(), event.userId(), event.errorMessage());

        cassandra.batchOps()
                .insert(event)
                .insert(attempt)
                .insert(delivery)
                .execute();
    }
}
