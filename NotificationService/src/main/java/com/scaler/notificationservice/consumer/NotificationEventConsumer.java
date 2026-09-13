package com.scaler.notificationservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.notificationservice.messaging.EventEnvelope;
import com.scaler.notificationservice.service.NotificationMessageFactory;
import com.scaler.notificationservice.service.NotificationSender;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.kafka.consumer-enabled", havingValue = "true", matchIfMissing = true)
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final NotificationMessageFactory factory;
    private final NotificationSender sender;

    @KafkaListener(
            topics = {
                "${notification.kafka.order-events-topic:order-events}",
                "${notification.kafka.payment-events-topic:payment-events}"
            },
            groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void onEvent(String message, Acknowledgment ack) {
        try {
            EventEnvelope env = objectMapper.readValue(message, EventEnvelope.class);
            factory.from(env).ifPresent(sender::send);
        } catch (Exception ex) {
            // No retry by design: log and drop so the message is not redelivered forever.
            log.error("Dropping notification event: {}", ex.getMessage());
        } finally {
            ack.acknowledge();
        }
    }
}
