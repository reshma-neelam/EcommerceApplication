package com.scaler.notificationservice.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.notificationservice.messaging.NotificationMessage;
import com.scaler.notificationservice.service.NotificationMessageFactory;
import com.scaler.notificationservice.service.NotificationSender;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.support.Acknowledgment;

class NotificationEventConsumerTest {

    private final NotificationSender sender = Mockito.mock(NotificationSender.class);
    private final Acknowledgment ack = Mockito.mock(Acknowledgment.class);
    private final NotificationEventConsumer consumer =
            new NotificationEventConsumer(new ObjectMapper(), new NotificationMessageFactory(), sender);

    @Test
    void validOrderCreatedEvent_sendsOnceAndAcknowledges() {
        String json = "{\"eventType\":\"OrderCreated.v1\",\"payload\":{\"email\":\"buyer@example.com\","
                + "\"orderId\":\"o1\",\"totalAmount\":\"10.00\",\"currency\":\"INR\"}}";

        consumer.onEvent(json, ack);

        verify(sender, times(1)).send(any(NotificationMessage.class));
        verify(ack).acknowledge();
    }

    @Test
    void malformedEvent_isDroppedButAcknowledged() {
        consumer.onEvent("not-json", ack);

        verify(sender, times(0)).send(any());
        verify(ack).acknowledge();
    }
}
