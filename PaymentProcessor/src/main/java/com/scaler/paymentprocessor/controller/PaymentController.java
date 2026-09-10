package com.scaler.paymentprocessor.controller;

import com.scaler.paymentprocessor.dto.payment.CreatePaymentRequestDTO;
import com.scaler.paymentprocessor.dto.payment.PaymentResponseDTO;
import com.scaler.paymentprocessor.security.AuthenticatedUser;
import com.scaler.paymentprocessor.service.PaymentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponseDTO> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequestDTO request) {
        PaymentResponseDTO created = paymentService.create(currentUserId(), idempotencyKey, request.getOrderId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDTO> get(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.getForUser(paymentId, currentUserId(), isAdmin()));
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
