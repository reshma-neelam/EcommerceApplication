package com.scaler.paymentprocessor.gateway;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatePaymentCommand {
    private UUID paymentId;
    private UUID orderId;
    private BigDecimal amount;
    private String currency;
    private String providerIdempotencyKey;
}
