package com.scaler.notificationservice.service;

import com.scaler.notificationservice.messaging.EventEnvelope;
import com.scaler.notificationservice.messaging.NotificationMessage;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageFactory {

    public Optional<NotificationMessage> from(EventEnvelope env) {
        Map<String, Object> p = env.getPayload();
        String email = p == null ? null : (String) p.get("email");
        String type = env.getEventType() == null ? "" : env.getEventType();

        if (type.startsWith("OrderCreated")) {
            return Optional.of(build(email, "Your order is placed",
                    "Order " + p.get("orderId") + " total " + p.get("totalAmount")
                            + " " + p.get("currency") + " is awaiting payment."));
        }
        if (type.startsWith("OrderStatusChanged")) {
            return Optional.of(build(email, "Order status update",
                    "Order " + p.get("orderId") + " is now " + p.get("status") + "."));
        }
        if (type.startsWith("PaymentFailed")) {
            return Optional.of(build(email, "Payment failed",
                    "Payment for order " + p.get("orderId") + " failed. Please retry."));
        }
        return Optional.empty(); // ignored event type
    }

    private NotificationMessage build(String to, String subject, String body) {
        return NotificationMessage.builder().recipient(to).subject(subject).body(body).build();
    }
}
