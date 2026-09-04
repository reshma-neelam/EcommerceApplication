package com.scaler.productcatalog.service;

import com.scaler.productcatalog.model.OutboxEvent;
import com.scaler.productcatalog.repository.OutboxEventRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Writes domain events to the transactional outbox in the caller's transaction. */
@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxEventRepository outboxEventRepository;

    public void write(String aggregateType, UUID aggregateId, String eventType, Map<String, Object> payload) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(payload);
        outboxEventRepository.save(event);
    }
}
