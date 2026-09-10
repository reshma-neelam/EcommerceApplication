package com.scaler.paymentprocessor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.scaler.paymentprocessor.enums.PaymentStatus;
import com.scaler.paymentprocessor.model.Payment;
import com.scaler.paymentprocessor.repository.PaymentRepository;
import com.scaler.paymentprocessor.repository.WebhookEventRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PaymentWebhookServiceTest {

    @Autowired
    private PaymentWebhookService webhookService;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private WebhookEventRepository webhookEventRepository;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void clean() {
        webhookEventRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    private Payment seedPayment(String intentId) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(UUID.randomUUID());
        payment.setUserId(userId);
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setCurrency("INR");
        payment.setAmount(new BigDecimal("120.0000"));
        payment.setIdempotencyKey("key-1");
        payment.setProviderPaymentIntentId(intentId);
        return paymentRepository.saveAndFlush(payment);
    }

    @Test
    void apply_succeeded_transitionsPaymentAndRecordsOneWebhook() {
        Payment payment = seedPayment("pi_test");

        webhookService.apply("evt_1", "payment_intent.succeeded", "pi_test", null);

        assertThat(paymentRepository.findById(payment.getId())).get()
                .satisfies(p -> assertThat(p.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED));
        assertThat(webhookEventRepository.count()).isEqualTo(1);
    }

    @Test
    void apply_duplicateEvent_isDeduplicated() {
        Payment payment = seedPayment("pi_test");

        webhookService.apply("evt_1", "payment_intent.succeeded", "pi_test", null);
        webhookService.apply("evt_1", "payment_intent.succeeded", "pi_test", null);

        assertThat(paymentRepository.findById(payment.getId())).get()
                .satisfies(p -> assertThat(p.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED));
        assertThat(webhookEventRepository.count()).isEqualTo(1);
    }

    @Test
    void apply_uncorrelatedIntent_storesReceivedRowAndNoPaymentChange() {
        Payment payment = seedPayment("pi_test");

        webhookService.apply("evt_2", "payment_intent.succeeded", "pi_unknown", null);

        assertThat(paymentRepository.findById(payment.getId())).get()
                .satisfies(p -> assertThat(p.getStatus()).isEqualTo(PaymentStatus.INITIATED));
        assertThat(webhookEventRepository.findByProviderEventId("evt_2")).get()
                .satisfies(w -> assertThat(w.getStatus()).isEqualTo("RECEIVED"));
    }
}
