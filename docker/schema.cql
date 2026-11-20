CREATE TABLE IF NOT EXISTS events_by_user_month (
    user_id text,
    month_bucket date,
    event_time timestamp,
    event_id uuid,
    notification_id uuid,
    event_type text,
    channel text,
    delivery_status text,
    message text,
    error_message text,
    PRIMARY KEY ((user_id, month_bucket), event_time, event_id)
) WITH CLUSTERING ORDER BY (event_time DESC, event_id DESC);

CREATE TABLE IF NOT EXISTS attempts_by_notification_month (
    notification_id uuid,
    month_bucket date,
    event_time timestamp,
    event_id uuid,
    user_id text,
    channel text,
    delivery_status text,
    error_message text,
    PRIMARY KEY ((notification_id, month_bucket), event_time, event_id)
) WITH CLUSTERING ORDER BY (event_time DESC, event_id DESC);

CREATE TABLE IF NOT EXISTS deliveries_by_channel_status_month (
    channel text,
    delivery_status text,
    month_bucket date,
    event_time timestamp,
    event_id uuid,
    notification_id uuid,
    user_id text,
    error_message text,
    PRIMARY KEY ((channel, delivery_status, month_bucket), event_time, event_id)
) WITH CLUSTERING ORDER BY (event_time DESC, event_id DESC);
