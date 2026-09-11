package com.scaler.orderprocessor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.orderprocessor.enums.ChangedByType;
import com.scaler.orderprocessor.enums.InboxStatus;
import com.scaler.orderprocessor.enums.OrderStatus;
import com.scaler.orderprocessor.messaging.EventEnvelope;
import com.scaler.orderprocessor.model.InboxEvent;
import com.scaler.orderprocessor.model.Order;
import com.scaler.orderprocessor.model.OrderStatusHistory;
import com.scaler.orderprocessor.repository.InboxEventRepository;
import com.scaler.orderprocessor.repository.OrderRepository;
import com.scaler.orderprocessor.repository.OrderStatusHistoryRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentEventHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventHandler.class);

    private final ObjectMapper objectMapper;
    private final InboxEventRepository inboxEventRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OutboxWriter outboxWriter;

    @Transactional
    public void handle(String message) {
        EventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(message, EventEnvelope.class);
        } catch (Exception ex) {
            log.error("Discarding unparseable payment event: {}", truncate(ex.getMessage()));
            return; // malformed -> ack (nothing to retry)
        }

        UUID eventId = UUID.fromString(envelope.getEventId());
        if (inboxEventRepository.existsById(eventId)) {
            return; // duplicate delivery -> idempotent no-op
        }

        InboxEvent inbox = new InboxEvent();
        inbox.setEventId(eventId);
        inbox.setSourceService(envelope.getSourceService());
        inbox.setEventType(envelope.getEventType());
        inbox.setPayload(envelope.getPayload());

        OrderStatus target = mapTarget(envelope.getEventType());
        if (target == null) {
            inbox.setStatus(InboxStatus.PROCESSED);
            inbox.setLastError("Ignored event type " + envelope.getEventType());
            inbox.setProcessedAt(Instant.now());
            inboxEventRepository.save(inbox);
            return;
        }

        UUID orderId = UUID.fromString((String) envelope.getPayload().get("orderId"));
        Optional<Order> maybe = orderRepository.findById(orderId);
        if (maybe.isEmpty()) {
            inbox.setStatus(InboxStatus.FAILED);
            inbox.setLastError("Order not found: " + orderId);
            inboxEventRepository.save(inbox);
            log.warn("Payment event {} references missing order {}", eventId, orderId);
            return; // ack: order-not-found is not retryable here
        }

        Order order = maybe.get();
        OrderStatus from = order.getStatus();
        if (!OrderStateMachine.canTransition(from, target)) {
            inbox.setStatus(InboxStatus.PROCESSED);
            inbox.setLastError("Illegal transition " + from + " -> " + target + " (quarantined)");
            inbox.setProcessedAt(Instant.now());
            inboxEventRepository.save(inbox);
            log.info("Quarantined out-of-order payment event {} for order {} ({} -> {})",
                    eventId, orderId, from, target);
            return;
        }

        order.setStatus(target);
        orderRepository.save(order);
        appendHistory(orderId, from, target,
                target == OrderStatus.CONFIRMED ? "PAYMENT_SUCCEEDED" : "PAYMENT_FAILED");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId.toString());
        payload.put("userId", order.getUserId().toString());
        payload.put("status", target.name());
        payload.put("currency", order.getCurrency());
        payload.put("totalAmount", order.getTotalAmount().toPlainString());
        outboxWriter.write(orderId, "OrderStatusChanged.v1", payload);

        inbox.setStatus(InboxStatus.PROCESSED);
        inbox.setProcessedAt(Instant.now());
        inboxEventRepository.save(inbox);
    }

    private OrderStatus mapTarget(String eventType) {
        if (eventType == null) {
            return null;
        }
        if (eventType.startsWith("PaymentSucceeded")) {
            return OrderStatus.CONFIRMED;
        }
        if (eventType.startsWith("PaymentFailed")) {
            return OrderStatus.PAYMENT_FAILED;
        }
        return null;
    }

    private void appendHistory(UUID orderId, OrderStatus from, OrderStatus to, String reasonCode) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrderId(orderId);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setReasonCode(reasonCode);
        h.setChangedByType(ChangedByType.PAYMENT_EVENT);
        h.setChangedById("payment-processor");
        historyRepository.save(h);
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }
}
