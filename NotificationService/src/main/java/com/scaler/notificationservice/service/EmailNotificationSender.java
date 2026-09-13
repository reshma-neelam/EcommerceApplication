package com.scaler.notificationservice.service;

import com.scaler.notificationservice.messaging.NotificationMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "notification.mail.enabled", havingValue = "true")
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    private final JavaMailSender mailSender;

    @Value("${notification.mail.from}")
    private String from;

    @Override
    public void send(NotificationMessage m) {
        if (m.getRecipient() == null || m.getRecipient().isBlank()) {
            log.warn("Skipping email: no recipient for subject '{}'", m.getSubject());
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(m.getRecipient());
        msg.setSubject(m.getSubject());
        msg.setText(m.getBody());
        mailSender.send(msg);
        log.info("Notification email sent to {}", m.getRecipient());
    }
}
