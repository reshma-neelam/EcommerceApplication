package com.scaler.paymentprocessor.client;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderPaymentDetailsDTO {
    private UUID orderId;
    private UUID userId;
    private String currency;
    private BigDecimal totalAmount;
    private boolean payable;
}
