package com.scaler.orderprocessor.dto.order;

import com.scaler.orderprocessor.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDTO {
    private UUID id;
    private String orderNumber;
    private UUID userId;
    private OrderStatus status;
    private String currency;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private List<OrderItemResponseDTO> items;
    private AddressDTO shippingAddress;
    private AddressDTO billingAddress;
    private Instant createdAt;
    private Instant updatedAt;
}
