package com.scaler.orderprocessor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.orderprocessor.enums.InboxStatus;
import com.scaler.orderprocessor.enums.OrderStatus;
import com.scaler.orderprocessor.messaging.EventEnvelope;
import com.scaler.orderprocessor.model.InboxEvent;
import com.scaler.orderprocessor.model.Order;
import com.scaler.orderprocessor.model.OutboxEvent;
import com.scaler.orderprocessor.repository.InboxEventRepository;
import com.scaler.orderprocessor.repository.OrderRepository;
import com.scaler.orderprocessor.repository.OrderStatusHistoryRepository;
import com.scaler.orderprocessor.repository.OutboxEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PaymentEventHandlerDedupTest {

    @Autowired
    private PaymentEventHandler handler;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderStatusHistoryRepository historyRepository;
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    @Autowired
    private InboxEventRepository inboxEventRepository;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void clean() {
        historyRepository.deleteAll();
        outboxEventRepository.deleteAll();
        inboxEventRepository.deleteAll();
        orderRepository.deleteAll();
    }

    private Order seedOrder(OrderStatus status) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setOrderNumber("ORD-" + System.nanoTime());
        order.setUserId(userId);
        order.setStatus(status);
        order.setCurrency("INR");
        order.setSubtotalAmount(new BigDecimal("120.0000"));
        order.setTotalAmount(new BigDecimal("120.0000"));
        return orderRepository.saveAndFlush(order);
    }

    private String envelopeJson(String eventId, String eventType, UUID orderId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId.toString());
        payload.put("userId", userId.toString());
        payload.put("status", "SUCCEEDED");
        payload.put("amount", "120.0000");
        payload.put("currency", "INR");
        EventEnvelope envelope = EventEnvelope.builder()
                .eventId(eventId)
                .eventType(eventType)
                .eventVersion(1)
                .sourceService("PaymentProcessor")
                .aggregateId(orderId.toString())
                .occurredAt(Instant.now())
                .payload(payload)
                .build();
        return objectMapper.writeValueAsString(envelope);
    }

    private long orderStatusChangedCount() {
        return outboxEventRepository.findAll().stream()
                .filter(e -> "OrderStatusChanged.v1".equals(e.getEventType()))
                .count();
    }

    @Test
    void duplicateEvent_producesOneTransitionAndOneHistoryRow() throws Exception {
        Order order = seedOrder(OrderStatus.PENDING_PAYMENT);
        String eventId = UUID.randomUUID().toString();
        String json = envelopeJson(eventId, "PaymentSucceeded.v1", order.getId());

        handler.handle(json);
        handler.handle(json);

        assertThat(orderRepository.findById(order.getId())).get()
                .satisfies(o -> assertThat(o.getStatus()).isEqualTo(OrderStatus.CONFIRMED));
        assertThat(historyRepository.findByOrderIdOrderByCreatedAtAsc(order.getId())).hasSize(1);
        assertThat(orderStatusChangedCount()).isEqualTo(1);
        assertThat(inboxEventRepository.findById(UUID.fromString(eventId))).get()
                .satisfies(i -> assertThat(i.getStatus()).isEqualTo(InboxStatus.PROCESSED));
    }
}
