package com.scaler.notificationservice.service;

import com.scaler.notificationservice.messaging.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "notification.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LogNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationSender.class);

    @Override
    public void send(NotificationMessage m) {
        log.info("[NOTIFICATION] to={} subject='{}' body='{}'",
                m.getRecipient(), m.getSubject(), m.getBody());
    }
}
