package com.scaler.paymentprocessor.gateway;

import com.scaler.paymentprocessor.exception.DependencyUnavailableException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class StripePaymentGateway implements PaymentGateway {

    @Override
    public CreatePaymentResult createPayment(CreatePaymentCommand command) {
        long minorUnits = toMinorUnits(command.getAmount());
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(minorUnits)
                .setCurrency(command.getCurrency().toLowerCase())
                .putMetadata("paymentId", command.getPaymentId().toString())
                .putMetadata("orderId", command.getOrderId().toString())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true).build())
                .build();
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey(command.getProviderIdempotencyKey())
                .build();
        try {
            PaymentIntent intent = PaymentIntent.create(params, options);
            return CreatePaymentResult.builder()
                    .providerPaymentIntentId(intent.getId())
                    .clientSecret(intent.getClientSecret())
                    .providerStatus(intent.getStatus())
                    .build();
        } catch (StripeException ex) {
            throw new DependencyUnavailableException("PAYMENT_PROVIDER_ERROR",
                    "Stripe could not create the payment");
        }
    }

    private long toMinorUnits(BigDecimal amount) {
        BigDecimal minor = amount.movePointRight(2).stripTrailingZeros();
        if (minor.scale() > 0) {
            throw new IllegalArgumentException("Amount has sub-minor-unit precision: " + amount);
        }
        return minor.longValueExact();
    }
}
