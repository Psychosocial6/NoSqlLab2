package com.example.nosqllab2.notifications;

import org.springframework.data.cassandra.repository.MapIdCassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeliveryAttemptRepository extends MapIdCassandraRepository<NotificationDeliveryAttempt> {

    @Query("""
            SELECT * FROM attempts_by_notification_month
            WHERE notification_id = ?0 AND month_bucket = ?1
            LIMIT 1
            """)
    Optional<NotificationDeliveryAttempt> findLatestAttempt(UUID notificationId, LocalDate monthBucket);

    @Query("""
            SELECT * FROM attempts_by_notification_month
            WHERE notification_id = ?0 AND month_bucket = ?1
              AND event_time >= ?2 AND event_time < ?3
            """)
    Slice<NotificationDeliveryAttempt> findAttempts(
            UUID notificationId, LocalDate monthBucket,
            Instant from, Instant to, Pageable pageable);
}
