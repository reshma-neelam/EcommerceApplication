package com.scaler.productcatalog.dto.inventory;

import java.time.Instant;
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
public class InventoryResponseDTO {
    private UUID productId;
    private int onHandQuantity;
    private int reservedQuantity;
    private int availableQuantity;
    private int reorderLevel;
    private long version;
    private Instant updatedAt;
}
