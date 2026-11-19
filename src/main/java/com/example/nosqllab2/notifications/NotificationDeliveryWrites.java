package com.example.nosqllab2.notifications;

interface NotificationDeliveryWrites {
    void saveDelivery(UserNotificationEvent event);
}
