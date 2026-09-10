package com.scaler.paymentprocessor.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    private final StripeProperties props;

    public StripeConfig(StripeProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        String key = props.getStripe().getSecretKey();
        if (key == null || key.startsWith("sk_live_")) {
            throw new IllegalStateException(
                    "Refusing to start: a non-test Stripe secret key was supplied. Use sk_test_ only.");
        }
        Stripe.apiKey = key;
    }
}
