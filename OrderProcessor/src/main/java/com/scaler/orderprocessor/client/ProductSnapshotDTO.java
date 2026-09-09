package com.scaler.orderprocessor.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductSnapshotDTO {
    private UUID id;
    private String sku;
    private String name;
    private String status;
    private BigDecimal basePrice;
    private String currency;
    private Integer availableQuantity;
}
