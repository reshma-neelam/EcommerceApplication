package com.scaler.paymentprocessor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "payment")
public class StripeProperties {
    private String currency = "INR";
    private final Stripe stripe = new Stripe();

    @Getter
    @Setter
    public static class Stripe {
        private String secretKey;
        private String webhookSecret;
    }
}
