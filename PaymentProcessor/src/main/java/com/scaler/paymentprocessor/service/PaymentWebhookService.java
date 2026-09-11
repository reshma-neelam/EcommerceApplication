package com.scaler.paymentprocessor.service;

import com.scaler.paymentprocessor.enums.PaymentStatus;
import com.scaler.paymentprocessor.model.Payment;
import com.scaler.paymentprocessor.model.WebhookEvent;
import com.scaler.paymentprocessor.repository.PaymentRepository;
import com.scaler.paymentprocessor.repository.WebhookEventRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentWebhookService {

    private final WebhookEventRepository webhookRepository;
    private final PaymentRepository paymentRepository;
    private final OutboxWriter outboxWriter;

    public PaymentWebhookService(WebhookEventRepository webhookRepository,
                                 PaymentRepository paymentRepository,
                                 OutboxWriter outboxWriter) {
        this.webhookRepository = webhookRepository;
        this.paymentRepository = paymentRepository;
        this.outboxWriter = outboxWriter;
    }

    /** eventType is one of payment_intent.succeeded / payment_intent.payment_failed. */
    @Transactional
    public void apply(String providerEventId, String eventType, String paymentIntentId, String failureReason) {
        if (webhookRepository.findByProviderEventId(providerEventId).isPresent()) {
            return; // duplicate delivery -> no-op
        }

        WebhookEvent event = new WebhookEvent();
        event.setId(UUID.randomUUID());
        event.setProviderEventId(providerEventId);
        event.setEventType(eventType);
        event.setStatus("PROCESSED");

        Optional<Payment> maybe = paymentRepository.findByProviderPaymentIntentId(paymentIntentId);
        if (maybe.isEmpty()) {
            event.setStatus("RECEIVED"); // reconciliation worker (Week 6) retries correlation
            webhookRepository.save(event);
            return;
        }

        Payment payment = maybe.get();
        PaymentStatus target = "payment_intent.succeeded".equals(eventType)
                ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        if (PaymentStateMachine.canTransition(payment.getStatus(), target)) {
            payment.setStatus(target);
            payment.setProviderStatus(eventType);
            if (target == PaymentStatus.FAILED) {
                payment.setFailureReason(failureReason);
            }
            paymentRepository.save(payment);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("paymentId", payment.getId().toString());
            payload.put("orderId", payment.getOrderId().toString());
            payload.put("userId", payment.getUserId().toString());
            payload.put("status", target.name());
            payload.put("amount", payment.getAmount().toPlainString());
            payload.put("currency", payment.getCurrency());
            if (target == PaymentStatus.FAILED && failureReason != null) {
                payload.put("failureReason", failureReason);
            }
            outboxWriter.write(payment.getOrderId(),
                    target == PaymentStatus.SUCCEEDED ? "PaymentSucceeded.v1" : "PaymentFailed.v1",
                    payload);
        }
        event.setPaymentId(payment.getId());
        event.setProcessedAt(Instant.now());
        webhookRepository.save(event);
    }
}
