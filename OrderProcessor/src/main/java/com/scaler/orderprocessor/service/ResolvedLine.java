package com.scaler.orderprocessor.service;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Internal computation carrier (not an API DTO) */
@Getter
@AllArgsConstructor
class ResolvedLine {
    private final UUID productId;
    private final String sku;
    private final String name;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final BigDecimal lineTotal;
}
