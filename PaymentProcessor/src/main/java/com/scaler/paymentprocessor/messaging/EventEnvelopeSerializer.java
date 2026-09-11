package com.scaler.paymentprocessor.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.paymentprocessor.model.OutboxEvent;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Serializes an outbox row into a versioned JSON envelope for Kafka. */
@Component
@RequiredArgsConstructor
public class EventEnvelopeSerializer {

    private static final String SOURCE_SERVICE = "PaymentProcessor";
    private final ObjectMapper objectMapper;

    public String toJson(OutboxEvent row) {
        int version = parseVersion(row.getEventType());
        EventEnvelope envelope = EventEnvelope.builder()
                .eventId(row.getId().toString())
                .eventType(row.getEventType())
                .eventVersion(version)
                .sourceService(SOURCE_SERVICE)
                .aggregateId(row.getAggregateId().toString())
                .occurredAt(Instant.now())
                .payload(row.getPayload())
                .build();
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize event envelope for outbox " + row.getId(), ex);
        }
    }

    private int parseVersion(String eventType) {
        int idx = eventType.lastIndexOf(".v");
        if (idx < 0) {
            return 1;
        }
        try {
            return Integer.parseInt(eventType.substring(idx + 2));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }
}
