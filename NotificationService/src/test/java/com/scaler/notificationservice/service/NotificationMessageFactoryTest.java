package com.scaler.notificationservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.scaler.notificationservice.messaging.EventEnvelope;
import com.scaler.notificationservice.messaging.NotificationMessage;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NotificationMessageFactoryTest {

    private final NotificationMessageFactory factory = new NotificationMessageFactory();

    private EventEnvelope envelope(String type, Map<String, Object> payload) {
        return EventEnvelope.builder().eventType(type).payload(payload).build();
    }

    @Test
    void orderCreated_producesMessageAddressedToPayloadEmail() {
        Optional<NotificationMessage> msg = factory.from(envelope("OrderCreated.v1", Map.of(
                "email", "buyer@example.com", "orderId", "o1", "totalAmount", "10.00", "currency", "INR")));

        assertThat(msg).isPresent();
        assertThat(msg.get().getRecipient()).isEqualTo("buyer@example.com");
        assertThat(msg.get().getSubject()).isEqualTo("Your order is placed");
    }

    @Test
    void orderStatusChanged_producesMessage() {
        Optional<NotificationMessage> msg = factory.from(envelope("OrderStatusChanged.v1", Map.of(
                "email", "buyer@example.com", "orderId", "o1", "status", "CONFIRMED")));

        assertThat(msg).isPresent();
        assertThat(msg.get().getSubject()).isEqualTo("Order status update");
    }

    @Test
    void paymentFailed_producesMessage() {
        Optional<NotificationMessage> msg = factory.from(envelope("PaymentFailed.v1", Map.of(
                "email", "buyer@example.com", "orderId", "o1")));

        assertThat(msg).isPresent();
        assertThat(msg.get().getSubject()).isEqualTo("Payment failed");
    }

    @Test
    void unknownType_isIgnored() {
        assertThat(factory.from(envelope("PaymentSucceeded.v1", Map.of("email", "x@y.com")))).isEmpty();
    }
}
