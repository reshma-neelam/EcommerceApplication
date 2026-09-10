package com.scaler.orderprocessor.dto.order;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PaymentDetailsResponseDTO {
    private UUID orderId;
    private UUID userId;
    private String currency;
    private BigDecimal totalAmount;
    private boolean payable;
}
