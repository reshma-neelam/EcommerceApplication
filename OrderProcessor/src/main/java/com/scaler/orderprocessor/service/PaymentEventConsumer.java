package com.scaler.orderprocessor.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "order.kafka.consumer-enabled", havingValue = "true", matchIfMissing = true)
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);
    private final PaymentEventHandler handler;

    @KafkaListener(
            topics = "${order.kafka.payment-events-topic:payment-events}",
            groupId = "${spring.kafka.consumer.group-id:order-processor}")
    public void onPaymentEvent(String message, Acknowledgment ack) {
        handler.handle(message);
        ack.acknowledge();
    }
}
