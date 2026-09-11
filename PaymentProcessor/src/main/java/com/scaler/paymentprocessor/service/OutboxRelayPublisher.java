package com.scaler.paymentprocessor.service;

import com.scaler.paymentprocessor.enums.OutboxStatus;
import com.scaler.paymentprocessor.messaging.EventEnvelopeSerializer;
import com.scaler.paymentprocessor.model.OutboxEvent;
import com.scaler.paymentprocessor.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventEnvelopeSerializer serializer;

    @Value("${payment.kafka.payment-events-topic:payment-events}")
    private String topic;

    @Value("${payment.outbox.relay.batch-size:50}")
    private int batchSize;

    @Value("${payment.outbox.relay.max-retries:10}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${payment.outbox.relay.poll-interval-ms:2000}")
    @Transactional
    public void relay() {
        List<OutboxEvent> batch = outboxEventRepository
                .findByStatusOrderByCreatedAtAsc(OutboxStatus.NEW, PageRequest.of(0, batchSize));
        for (OutboxEvent row : batch) {
            try {
                String value = serializer.toJson(row);
                kafkaTemplate.send(topic, row.getAggregateId().toString(), value).get();
                row.setStatus(OutboxStatus.PUBLISHED);
                row.setPublishedAt(Instant.now());
                row.setLastError(null);
            } catch (Exception ex) {
                row.setRetryCount(row.getRetryCount() + 1);
                row.setLastError(truncate(ex.getMessage()));
                if (row.getRetryCount() >= maxRetries) {
                    row.setStatus(OutboxStatus.FAILED);
                }
            }
            outboxEventRepository.save(row);
        }
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
