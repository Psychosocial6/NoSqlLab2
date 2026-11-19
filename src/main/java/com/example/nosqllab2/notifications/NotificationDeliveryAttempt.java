package com.example.nosqllab2.notifications;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Table("attempts_by_notification_month")
public record NotificationDeliveryAttempt(
        @PrimaryKeyColumn(name = "notification_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
        UUID notificationId,
        @PrimaryKeyColumn(name = "month_bucket", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
        LocalDate monthBucket,
        @PrimaryKeyColumn(name = "event_time", ordinal = 2,
                type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
        Instant eventTime,
        @PrimaryKeyColumn(name = "event_id", ordinal = 3,
                type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
        UUID eventId,
        @Column("user_id") String userId,
        String channel,
        @Column("delivery_status") String deliveryStatus,
        @Column("error_message") String errorMessage
) {
}
