package com.scaler.paymentprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.scaler.paymentprocessor.enums.OutboxStatus;
import com.scaler.paymentprocessor.enums.PaymentStatus;
import com.scaler.paymentprocessor.model.OutboxEvent;
import com.scaler.paymentprocessor.model.Payment;
import com.scaler.paymentprocessor.repository.OutboxEventRepository;
import com.scaler.paymentprocessor.repository.PaymentRepository;
import com.scaler.paymentprocessor.service.OutboxRelayPublisher;
import com.scaler.paymentprocessor.service.PaymentWebhookService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Proves publish-after-commit durability: {@code PaymentWebhookService.apply(...)} commits the
 * payment row and a NEW outbox row independently of Kafka; a subsequent relay pass publishes the
 * row to {@code payment-events} and marks it PUBLISHED. The scheduled relay is parked (very long
 * poll interval) so the test drives {@code relay()} deterministically. Requires a Docker engine.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class OutboxRelayRecoveryIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("payment_processor_db");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("payment.stripe.secret-key", () -> "sk_test_dummy");
        registry.add("payment.stripe.webhook-secret", () -> "whsec_test_dummy");
        // Park the scheduled relay so the test triggers relay() manually.
        registry.add("payment.outbox.relay.poll-interval-ms", () -> "3600000");
    }

    @Autowired
    private PaymentWebhookService webhookService;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    @Autowired
    private OutboxRelayPublisher relayPublisher;

    @Test
    void businessCommitsThenRelayPublishes() {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(UUID.randomUUID());
        payment.setUserId(UUID.randomUUID());
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setCurrency("INR");
        payment.setAmount(new BigDecimal("120.0000"));
        payment.setIdempotencyKey("key-" + System.nanoTime());
        payment.setProviderPaymentIntentId("pi_test_recovery");
        paymentRepository.saveAndFlush(payment);

        // Phase A: business fact commits with a NEW outbox row, independent of Kafka.
        webhookService.apply("evt_recovery", "payment_intent.succeeded", "pi_test_recovery", null);

        assertThat(paymentRepository.findById(payment.getId())).get()
                .satisfies(p -> assertThat(p.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED));
        List<OutboxEvent> newRows =
                outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.NEW, org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(newRows).anySatisfy(r -> assertThat(r.getEventType()).isEqualTo("PaymentSucceeded.v1"));

        // Phase B: relay publishes the row to the live broker and marks it PUBLISHED.
        relayPublisher.relay();

        assertThat(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.NEW,
                org.springframework.data.domain.PageRequest.of(0, 10))).isEmpty();
        assertThat(outboxEventRepository.findAll()).anySatisfy(r ->
                assertThat(r.getStatus()).isEqualTo(OutboxStatus.PUBLISHED));
    }
}
