package com.scaler.paymentprocessor.controller;

import com.scaler.paymentprocessor.config.StripeProperties;
import com.scaler.paymentprocessor.service.PaymentWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {

    private final PaymentWebhookService webhookService;
    private final String webhookSecret;

    public StripeWebhookController(PaymentWebhookService webhookService, StripeProperties props) {
        this.webhookService = webhookService;
        this.webhookSecret = props.getStripe().getWebhookSecret();
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody String payload,
                                        @RequestHeader("Stripe-Signature") String signature) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException ex) {
            return ResponseEntity.badRequest().build();
        }

        String type = event.getType();
        if ("payment_intent.succeeded".equals(type) || "payment_intent.payment_failed".equals(type)) {
            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject().orElse(null);
            if (intent != null) {
                String failureReason = intent.getLastPaymentError() != null
                        ? intent.getLastPaymentError().getMessage() : null;
                webhookService.apply(event.getId(), type, intent.getId(), failureReason);
            }
        }
        return ResponseEntity.ok().build();
    }
}
