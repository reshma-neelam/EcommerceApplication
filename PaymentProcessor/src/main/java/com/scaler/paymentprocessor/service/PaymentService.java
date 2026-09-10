package com.scaler.paymentprocessor.service;

import com.scaler.paymentprocessor.client.OrderPaymentDetailsClient;
import com.scaler.paymentprocessor.client.OrderPaymentDetailsDTO;
import com.scaler.paymentprocessor.config.StripeProperties;
import com.scaler.paymentprocessor.dto.payment.PaymentResponseDTO;
import com.scaler.paymentprocessor.enums.PaymentStatus;
import com.scaler.paymentprocessor.exception.BadRequestException;
import com.scaler.paymentprocessor.exception.ConflictException;
import com.scaler.paymentprocessor.exception.NotFoundException;
import com.scaler.paymentprocessor.gateway.CreatePaymentCommand;
import com.scaler.paymentprocessor.gateway.CreatePaymentResult;
import com.scaler.paymentprocessor.gateway.PaymentGateway;
import com.scaler.paymentprocessor.model.Payment;
import com.scaler.paymentprocessor.repository.PaymentRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderPaymentDetailsClient orderClient;
    private final PaymentGateway paymentGateway;
    private final String configuredCurrency;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderPaymentDetailsClient orderClient,
                          PaymentGateway paymentGateway,
                          StripeProperties props) {
        this.paymentRepository = paymentRepository;
        this.orderClient = orderClient;
        this.paymentGateway = paymentGateway;
        this.configuredCurrency = props.getCurrency();
    }

    @Transactional
    public PaymentResponseDTO create(UUID userId, String idempotencyKey, UUID orderId) {
        Optional<Payment> existing = paymentRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getOrderId().equals(orderId)) {
                throw new ConflictException("DUPLICATE_REQUEST",
                        "Idempotency key reused with a different order");
            }
            return toDto(existing.get(), null);
        }

        OrderPaymentDetailsDTO order = orderClient.fetch(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new NotFoundException("ORDER_NOT_FOUND", "Order not found: " + orderId);
        }
        if (!order.isPayable()) {
            throw new ConflictException("ORDER_NOT_PAYABLE", "Order is not payable: " + orderId);
        }
        if (!configuredCurrency.equalsIgnoreCase(order.getCurrency())) {
            throw new BadRequestException("UNSUPPORTED_CURRENCY",
                    "Unsupported currency: " + order.getCurrency());
        }
        paymentRepository.findByOrderId(orderId).ifPresent(p -> {
            throw new ConflictException("PAYMENT_EXISTS", "A payment already exists for order " + orderId);
        });

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setCurrency(order.getCurrency());
        payment.setAmount(order.getTotalAmount());
        payment.setIdempotencyKey(idempotencyKey);
        paymentRepository.saveAndFlush(payment);

        CreatePaymentResult result = paymentGateway.createPayment(CreatePaymentCommand.builder()
                .paymentId(payment.getId())
                .orderId(orderId)
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .providerIdempotencyKey(payment.getId().toString())
                .build());

        payment.setProviderPaymentIntentId(result.getProviderPaymentIntentId());
        payment.setProviderStatus(result.getProviderStatus());
        paymentRepository.save(payment);

        return toDto(payment, result.getClientSecret());
    }

    @Transactional(readOnly = true)
    public PaymentResponseDTO getForUser(UUID paymentId, UUID userId, boolean isAdmin) {
        Payment payment = paymentRepository.findById(paymentId)
                .filter(p -> isAdmin || p.getUserId().equals(userId))
                .orElseThrow(() -> new NotFoundException("RESOURCE_NOT_FOUND",
                        "Payment not found: " + paymentId));
        return toDto(payment, null);
    }

    private PaymentResponseDTO toDto(Payment p, String clientSecret) {
        return PaymentResponseDTO.builder()
                .id(p.getId())
                .orderId(p.getOrderId())
                .status(p.getStatus())
                .currency(p.getCurrency())
                .amount(p.getAmount())
                .providerStatus(p.getProviderStatus())
                .clientSecret(clientSecret)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
