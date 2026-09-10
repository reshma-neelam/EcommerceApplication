package com.scaler.orderprocessor.controller;

import com.scaler.orderprocessor.dto.order.PaymentDetailsResponseDTO;
import com.scaler.orderprocessor.service.OrderService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;

    @GetMapping("/{orderId}/payment-details")
    public ResponseEntity<PaymentDetailsResponseDTO> paymentDetails(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getPaymentDetails(orderId));
    }
}
