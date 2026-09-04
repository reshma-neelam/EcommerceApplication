package com.scaler.productcatalog.controller;

import com.scaler.productcatalog.dto.inventory.InventoryAdjustRequestDTO;
import com.scaler.productcatalog.dto.inventory.InventoryResponseDTO;
import com.scaler.productcatalog.dto.inventory.ReservationRequestDTO;
import com.scaler.productcatalog.dto.inventory.ReservationResponseDTO;
import com.scaler.productcatalog.service.InventoryService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal inventory operations for the Order Processor and admins. Network/credential
 * restriction is applied in Week 3 (Gate G) once JWT/service auth exists.
 */
@RestController
@RequestMapping("/internal/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PutMapping("/{productId}")
    public ResponseEntity<InventoryResponseDTO> adjust(@PathVariable UUID productId,
                                                       @Valid @RequestBody InventoryAdjustRequestDTO request) {
        return ResponseEntity.ok(inventoryService.adjust(productId, request));
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponseDTO> reserve(@Valid @RequestBody ReservationRequestDTO request) {
        return ResponseEntity.status(201).body(inventoryService.reserve(request));
    }

    @DeleteMapping("/reservations/{orderId}")
    public ResponseEntity<Void> release(@PathVariable UUID orderId) {
        inventoryService.release(orderId);
        return ResponseEntity.noContent().build();
    }
}
