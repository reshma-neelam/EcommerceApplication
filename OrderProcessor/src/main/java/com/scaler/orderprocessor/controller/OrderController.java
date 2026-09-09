package com.scaler.orderprocessor.controller;

import com.scaler.orderprocessor.dto.order.OrderCreateRequestDTO;
import com.scaler.orderprocessor.dto.order.OrderResponseDTO;
import com.scaler.orderprocessor.dto.order.OrderStatusResponseDTO;
import com.scaler.orderprocessor.security.AuthenticatedUser;
import com.scaler.orderprocessor.service.OrderService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponseDTO> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OrderCreateRequestDTO request) {
        OrderResponseDTO created = orderService.create(currentUserId(), idempotencyKey, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> get(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getForUser(orderId, currentUserId(), isAdmin()));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<OrderResponseDTO>> list(Pageable pageable) {
        return ResponseEntity.ok(orderService.listForUser(currentUserId(), pageable));
    }

    @GetMapping("/{orderId}/status")
    public ResponseEntity<OrderStatusResponseDTO> status(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getStatus(orderId, currentUserId(), isAdmin()));
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ((AuthenticatedUser) auth.getPrincipal()).getUserId();
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
