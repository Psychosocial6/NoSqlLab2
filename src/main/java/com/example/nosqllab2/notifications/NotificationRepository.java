package com.example.nosqllab2.notifications;

import org.springframework.data.cassandra.repository.MapIdCassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.Instant;
import java.time.LocalDate;

public interface NotificationRepository
        extends MapIdCassandraRepository<UserNotificationEvent>, NotificationDeliveryWrites {

    @Query("""
            SELECT * FROM events_by_user_month
            WHERE user_id = ?0 AND month_bucket = ?1
              AND event_time >= ?2 AND event_time < ?3
            """)
    Slice<UserNotificationEvent> findHistory(
            String userId, LocalDate monthBucket, Instant from, Instant to, Pageable pageable);
}
