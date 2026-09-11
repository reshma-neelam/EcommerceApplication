package com.scaler.paymentprocessor.messaging;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventEnvelope {
    private String eventId;
    private String eventType;
    private int eventVersion;
    private String sourceService;
    private String aggregateId;
    private Instant occurredAt;
    private String correlationId;
    private Map<String, Object> payload;
}
