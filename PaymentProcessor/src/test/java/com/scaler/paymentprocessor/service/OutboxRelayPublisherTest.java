package com.scaler.paymentprocessor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.scaler.paymentprocessor.enums.OutboxStatus;
import com.scaler.paymentprocessor.messaging.EventEnvelopeSerializer;
import com.scaler.paymentprocessor.model.OutboxEvent;
import com.scaler.paymentprocessor.repository.OutboxEventRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OutboxRelayPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private EventEnvelopeSerializer serializer;

    @InjectMocks
    private OutboxRelayPublisher relay;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(relay, "topic", "payment-events");
        ReflectionTestUtils.setField(relay, "batchSize", 50);
        ReflectionTestUtils.setField(relay, "maxRetries", 3);
    }

    private OutboxEvent newRow() {
        OutboxEvent row = new OutboxEvent();
        row.setId(UUID.randomUUID());
        row.setAggregateId(UUID.randomUUID());
        row.setEventType("PaymentSucceeded.v1");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", UUID.randomUUID().toString());
        row.setPayload(payload);
        return row;
    }

    @Test
    void publishFailure_incrementsRetryAndParksAsFailedAfterMaxRetries() {
        OutboxEvent row = newRow();
        row.setRetryCount(2); // one more failure reaches maxRetries=3
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                org.mockito.ArgumentMatchers.eq(OutboxStatus.NEW), any(Pageable.class)))
                .thenReturn(List.of(row));
        when(serializer.toJson(any(OutboxEvent.class))).thenReturn("{}");
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("broker down"));

        relay.relay();

        assertThat(row.getRetryCount()).isEqualTo(3);
        assertThat(row.getLastError()).contains("broker down");
        assertThat(row.getStatus()).isEqualTo(OutboxStatus.FAILED);
    }
}
