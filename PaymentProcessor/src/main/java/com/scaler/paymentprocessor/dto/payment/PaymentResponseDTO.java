package com.scaler.paymentprocessor.dto.payment;

import com.scaler.paymentprocessor.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PaymentResponseDTO {
    private UUID id;
    private UUID orderId;
    private PaymentStatus status;
    private String currency;
    private BigDecimal amount;
    private String clientSecret;
    private String providerStatus;
    private Instant createdAt;
}
