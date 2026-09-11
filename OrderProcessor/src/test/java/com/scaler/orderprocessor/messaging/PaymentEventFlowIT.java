package com.scaler.orderprocessor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.orderprocessor.enums.OrderStatus;
import com.scaler.orderprocessor.model.Order;
import com.scaler.orderprocessor.repository.InboxEventRepository;
import com.scaler.orderprocessor.repository.OrderRepository;
import com.scaler.orderprocessor.repository.OrderStatusHistoryRepository;
import com.scaler.orderprocessor.repository.OutboxEventRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end Week 6 happy path against real MySQL + Kafka containers: a
 * {@code PaymentSucceeded.v1} envelope on {@code payment-events} drives the seeded
 * order from PENDING_PAYMENT to CONFIRMED via the inbox-deduplicated consumer, appends
 * one status-history row, and writes {@code OrderStatusChanged.v1} to the outbox.
 * Runs under {@code mvn verify} (failsafe) and requires a running Docker engine.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PaymentEventFlowIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("order_processor_db");

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
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
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

    @Test
    void paymentSucceeded_confirmsOrder() throws Exception {
        UUID userId = UUID.randomUUID();
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setOrderNumber("ORD-" + System.nanoTime());
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCurrency("INR");
        order.setSubtotalAmount(new BigDecimal("120.0000"));
        order.setTotalAmount(new BigDecimal("120.0000"));
        orderRepository.saveAndFlush(order);

        String eventId = UUID.randomUUID().toString();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.getId().toString());
        payload.put("userId", userId.toString());
        payload.put("status", "SUCCEEDED");
        payload.put("amount", "120.0000");
        payload.put("currency", "INR");
        EventEnvelope envelope = EventEnvelope.builder()
                .eventId(eventId)
                .eventType("PaymentSucceeded.v1")
                .eventVersion(1)
                .sourceService("PaymentProcessor")
                .aggregateId(order.getId().toString())
                .occurredAt(Instant.now())
                .payload(payload)
                .build();

        kafkaTemplate.send("payment-events", order.getId().toString(),
                objectMapper.writeValueAsString(envelope)).get();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(orderRepository.findById(order.getId())).get()
                        .satisfies(o -> assertThat(o.getStatus()).isEqualTo(OrderStatus.CONFIRMED)));

        assertThat(historyRepository.findByOrderIdOrderByCreatedAtAsc(order.getId())).hasSize(1);
        assertThat(inboxEventRepository.findById(UUID.fromString(eventId))).isPresent();
        assertThat(outboxEventRepository.findAll().stream()
                .anyMatch(e -> "OrderStatusChanged.v1".equals(e.getEventType()))).isTrue();
    }
}
