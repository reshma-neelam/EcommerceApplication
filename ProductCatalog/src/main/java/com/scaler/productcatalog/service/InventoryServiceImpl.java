package com.scaler.productcatalog.service;

import com.scaler.productcatalog.dto.inventory.InventoryAdjustRequestDTO;
import com.scaler.productcatalog.dto.inventory.InventoryResponseDTO;
import com.scaler.productcatalog.dto.inventory.ReservationRequestDTO;
import com.scaler.productcatalog.dto.inventory.ReservationResponseDTO;
import com.scaler.productcatalog.enums.ReservationStatus;
import com.scaler.productcatalog.exception.BadRequestException;
import com.scaler.productcatalog.exception.ConflictException;
import com.scaler.productcatalog.exception.NotFoundException;
import com.scaler.productcatalog.mapper.InventoryMapper;
import com.scaler.productcatalog.model.InventoryReservation;
import com.scaler.productcatalog.model.ProductInventory;
import com.scaler.productcatalog.repository.InventoryReservationRepository;
import com.scaler.productcatalog.repository.ProductInventoryRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final String AGG_INVENTORY = "INVENTORY_RESERVATION";
    private static final String AGG_PRODUCT = "PRODUCT";
    private static final int DEFAULT_EXPIRY_MINUTES = 30;

    private final ProductInventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final OutboxWriter outboxWriter;

    @Override
    @Transactional
    public InventoryResponseDTO adjust(UUID productId, InventoryAdjustRequestDTO request) {
        ProductInventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("INVENTORY_NOT_FOUND", "Inventory not found: " + productId));
        if (inventory.getVersion() != request.getExpectedVersion()) {
            throw new ConflictException("OPTIMISTIC_LOCK",
                    "Inventory was modified concurrently. Retry with the latest version.");
        }
        if (request.getOnHandQuantity() < inventory.getReservedQuantity()) {
            throw new BadRequestException("INVALID_ON_HAND",
                    "onHandQuantity cannot be less than currently reserved quantity");
        }
        inventory.setOnHandQuantity(request.getOnHandQuantity());
        if (request.getReorderLevel() != null) {
            inventory.setReorderLevel(request.getReorderLevel());
        }
        inventory = inventoryRepository.save(inventory);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productId", productId.toString());
        payload.put("onHandQuantity", inventory.getOnHandQuantity());
        outboxWriter.write(AGG_PRODUCT, productId, "InventoryAdjusted.v1", payload);

        return InventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional
    public ReservationResponseDTO reserve(ReservationRequestDTO request) {
        Map<UUID, Integer> requested = toQuantityMap(request.getLines());

        List<InventoryReservation> existing = reservationRepository.findByOrderId(request.getOrderId());
        if (!existing.isEmpty()) {
            Map<UUID, Integer> existingMap = new LinkedHashMap<>();
            existing.forEach(r -> existingMap.put(r.getProductId(), r.getQuantity()));
            if (existingMap.equals(requested)) {
                return toResponse(request.getOrderId(), existing);
            }
            throw new ConflictException("DUPLICATE_REQUEST",
                    "Reservation for this orderId already exists with different contents");
        }

        Instant expiresAt = Instant.now().plus(
                request.getExpiresInMinutes() != null ? request.getExpiresInMinutes() : DEFAULT_EXPIRY_MINUTES,
                ChronoUnit.MINUTES);

        List<InventoryReservation> created = requested.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().toString()))
                .map(entry -> reserveLine(request.getOrderId(), entry.getKey(), entry.getValue(), expiresAt))
                .toList();

        return toResponse(request.getOrderId(), created);
    }

    @Override
    @Transactional
    public void release(UUID orderId) {
        List<InventoryReservation> active =
                reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE);
        for (InventoryReservation reservation : active) {
            releaseReservation(reservation, ReservationStatus.RELEASED, "InventoryReleased.v1");
        }
    }

    @Override
    @Transactional
    public int expireDue(Instant cutoff) {
        List<InventoryReservation> due =
                reservationRepository.findByStatusAndExpiresAtBefore(ReservationStatus.ACTIVE, cutoff);
        for (InventoryReservation reservation : due) {
            releaseReservation(reservation, ReservationStatus.EXPIRED, "InventoryReservationExpired.v1");
        }
        return due.size();
    }

    // ---------- Helpers ----------

    private InventoryReservation reserveLine(UUID orderId, UUID productId, int quantity, Instant expiresAt) {
        ProductInventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found: " + productId));
        if (inventory.getAvailableQuantity() < quantity) {
            throw new ConflictException("OUT_OF_STOCK",
                    "Insufficient available stock for product: " + productId);
        }
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);

        InventoryReservation reservation = new InventoryReservation();
        reservation.setOrderId(orderId);
        reservation.setProductId(productId);
        reservation.setQuantity(quantity);
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setExpiresAt(expiresAt);
        reservation = reservationRepository.save(reservation);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reservationId", reservation.getId().toString());
        payload.put("orderId", orderId.toString());
        payload.put("productId", productId.toString());
        payload.put("quantity", quantity);
        outboxWriter.write(AGG_INVENTORY, reservation.getId(), "InventoryReserved.v1", payload);

        return reservation;
    }

    private void releaseReservation(InventoryReservation reservation, ReservationStatus terminalStatus,
                                    String eventType) {
        ProductInventory inventory = inventoryRepository.findByProductIdForUpdate(reservation.getProductId())
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND",
                        "Product not found: " + reservation.getProductId()));
        inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - reservation.getQuantity()));
        inventoryRepository.save(inventory);

        reservation.setStatus(terminalStatus);
        reservationRepository.save(reservation);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reservationId", reservation.getId().toString());
        payload.put("orderId", reservation.getOrderId().toString());
        payload.put("productId", reservation.getProductId().toString());
        payload.put("quantity", reservation.getQuantity());
        outboxWriter.write(AGG_INVENTORY, reservation.getId(), eventType, payload);
    }

    private Map<UUID, Integer> toQuantityMap(List<ReservationRequestDTO.Line> lines) {
        Map<UUID, Integer> map = new LinkedHashMap<>();
        for (ReservationRequestDTO.Line line : lines) {
            if (map.putIfAbsent(line.getProductId(), line.getQuantity()) != null) {
                throw new BadRequestException("DUPLICATE_PRODUCT_LINE",
                        "Duplicate productId in reservation request: " + line.getProductId());
            }
        }
        return map;
    }

    private ReservationResponseDTO toResponse(UUID orderId, List<InventoryReservation> reservations) {
        List<ReservationResponseDTO.Line> lines = reservations.stream()
                .map(r -> ReservationResponseDTO.Line.builder()
                        .reservationId(r.getId())
                        .productId(r.getProductId())
                        .quantity(r.getQuantity())
                        .status(r.getStatus())
                        .expiresAt(r.getExpiresAt())
                        .build())
                .toList();
        return ReservationResponseDTO.builder().orderId(orderId).lines(lines).build();
    }
}
