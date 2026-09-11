package com.scaler.paymentprocessor.service;

import com.scaler.paymentprocessor.model.OutboxEvent;
import com.scaler.paymentprocessor.repository.OutboxEventRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Writes payment domain events to the transactional outbox in the caller's transaction. */
@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxEventRepository outboxEventRepository;

    public void write(UUID orderId, String eventType, Map<String, Object> payload) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("PAYMENT");
        event.setAggregateId(orderId);
        event.setEventType(eventType);
        event.setPayload(payload);
        outboxEventRepository.save(event);
    }
}
