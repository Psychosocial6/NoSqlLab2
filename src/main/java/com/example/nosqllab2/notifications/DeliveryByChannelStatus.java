package com.example.nosqllab2.notifications;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Table("deliveries_by_channel_status_month")
public record DeliveryByChannelStatus(
        @PrimaryKeyColumn(name = "channel", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
        String channel,
        @PrimaryKeyColumn(name = "delivery_status", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
        String deliveryStatus,
        @PrimaryKeyColumn(name = "month_bucket", ordinal = 2, type = PrimaryKeyType.PARTITIONED)
        LocalDate monthBucket,
        @PrimaryKeyColumn(name = "event_time", ordinal = 3,
                type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
        Instant eventTime,
        @PrimaryKeyColumn(name = "event_id", ordinal = 4,
                type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
        UUID eventId,
        @Column("notification_id") UUID notificationId,
        @Column("user_id") String userId,
        @Column("error_message") String errorMessage
) {
}
