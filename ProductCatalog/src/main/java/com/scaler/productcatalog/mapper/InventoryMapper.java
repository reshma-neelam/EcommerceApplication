package com.scaler.productcatalog.mapper;

import com.scaler.productcatalog.dto.inventory.InventoryResponseDTO;
import com.scaler.productcatalog.model.ProductInventory;

public final class InventoryMapper {

    private InventoryMapper() {
    }

    public static InventoryResponseDTO toResponse(ProductInventory inventory) {
        return InventoryResponseDTO.builder()
                .productId(inventory.getProductId())
                .onHandQuantity(inventory.getOnHandQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .reorderLevel(inventory.getReorderLevel())
                .version(inventory.getVersion())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}
