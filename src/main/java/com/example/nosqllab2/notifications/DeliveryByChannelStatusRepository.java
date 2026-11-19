package com.example.nosqllab2.notifications;

import org.springframework.data.cassandra.repository.MapIdCassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.Instant;
import java.time.LocalDate;

public interface DeliveryByChannelStatusRepository extends MapIdCassandraRepository<DeliveryByChannelStatus> {

    @Query("""
            SELECT * FROM deliveries_by_channel_status_month
            WHERE channel = ?0 AND delivery_status = ?1 AND month_bucket = ?2
              AND event_time >= ?3 AND event_time < ?4
            """)
    Slice<DeliveryByChannelStatus> findDeliveries(
            String channel, String deliveryStatus, LocalDate monthBucket,
            Instant from, Instant to, Pageable pageable);
}
