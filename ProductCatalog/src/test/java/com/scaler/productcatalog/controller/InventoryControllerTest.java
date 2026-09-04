package com.scaler.productcatalog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.productcatalog.dto.inventory.InventoryResponseDTO;
import com.scaler.productcatalog.dto.inventory.ReservationResponseDTO;
import com.scaler.productcatalog.enums.ReservationStatus;
import com.scaler.productcatalog.exception.ConflictException;
import com.scaler.productcatalog.service.InventoryService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void adjust_returns200() throws Exception {
        UUID productId = UUID.randomUUID();
        when(inventoryService.adjust(any(), any()))
                .thenReturn(InventoryResponseDTO.builder()
                        .productId(productId)
                        .onHandQuantity(100)
                        .reservedQuantity(0)
                        .availableQuantity(100)
                        .reorderLevel(5)
                        .version(1L)
                        .updatedAt(Instant.now())
                        .build());

        Map<String, Object> body = Map.of("onHandQuantity", 100, "reorderLevel", 5, "expectedVersion", 0);
        mockMvc.perform(put("/internal/v1/inventory/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(100));
    }

    @Test
    void reserve_returns201() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ReservationResponseDTO response = ReservationResponseDTO.builder()
                .orderId(orderId)
                .lines(List.of(ReservationResponseDTO.Line.builder()
                        .reservationId(UUID.randomUUID())
                        .productId(productId)
                        .quantity(2)
                        .status(ReservationStatus.ACTIVE)
                        .expiresAt(Instant.now())
                        .build()))
                .build();
        when(inventoryService.reserve(any())).thenReturn(response);

        Map<String, Object> body = Map.of(
                "orderId", orderId.toString(),
                "lines", List.of(Map.of("productId", productId.toString(), "quantity", 2)));
        mockMvc.perform(post("/internal/v1/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()));
    }

    @Test
    void reserve_withEmptyLines_returns400() throws Exception {
        UUID orderId = UUID.randomUUID();
        Map<String, Object> body = Map.of("orderId", orderId.toString(), "lines", List.of());
        mockMvc.perform(post("/internal/v1/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void reserve_whenDuplicateDifferentBody_returns409() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(inventoryService.reserve(any()))
                .thenThrow(new ConflictException("DUPLICATE_REQUEST", "different contents"));

        Map<String, Object> body = Map.of(
                "orderId", orderId.toString(),
                "lines", List.of(Map.of("productId", productId.toString(), "quantity", 2)));
        mockMvc.perform(post("/internal/v1/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_REQUEST"));
    }

    @Test
    void release_returns204() throws Exception {
        UUID orderId = UUID.randomUUID();
        mockMvc.perform(delete("/internal/v1/inventory/reservations/{orderId}", orderId))
                .andExpect(status().isNoContent());
        verify(inventoryService).release(orderId);
    }
}
