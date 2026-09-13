package com.scaler.notificationservice.service;

import com.scaler.notificationservice.messaging.NotificationMessage;

public interface NotificationSender {
    void send(NotificationMessage message);
}
