package com.scaler.paymentprocessor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "payment.order")
public class OrderClientProperties {
    private String baseUrl;
    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 3000;
}
