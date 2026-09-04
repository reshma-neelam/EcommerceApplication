package com.scaler.productcatalog.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class InventoryAdjustRequestDTO {

    @NotNull
    @Min(value = 0, message = "onHandQuantity must be >= 0")
    private Integer onHandQuantity;

    @Min(value = 0, message = "reorderLevel must be >= 0")
    private Integer reorderLevel;

    @NotNull(message = "expectedVersion is required")
    private Long expectedVersion;
}
