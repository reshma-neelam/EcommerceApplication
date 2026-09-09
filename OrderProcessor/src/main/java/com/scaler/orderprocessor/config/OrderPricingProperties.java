package com.scaler.orderprocessor.config;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "order.pricing")
public class OrderPricingProperties {
    private String currency = "INR";
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal shippingAmount = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
}
