package com.scaler.productcatalog.service;

import com.scaler.productcatalog.dto.inventory.InventoryAdjustRequestDTO;
import com.scaler.productcatalog.dto.inventory.InventoryResponseDTO;
import com.scaler.productcatalog.dto.inventory.ReservationRequestDTO;
import com.scaler.productcatalog.dto.inventory.ReservationResponseDTO;
import java.time.Instant;
import java.util.UUID;

public interface InventoryService {

    InventoryResponseDTO adjust(UUID productId, InventoryAdjustRequestDTO request);

    ReservationResponseDTO reserve(ReservationRequestDTO request);

    void release(UUID orderId);

    /** Expires all reservations already past their expiry. Returns the number expired. */
    int expireDue(Instant cutoff);
}
