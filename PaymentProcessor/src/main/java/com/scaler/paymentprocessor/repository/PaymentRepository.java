package com.scaler.paymentprocessor.repository;

import com.scaler.paymentprocessor.model.Payment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
    Optional<Payment> findByOrderId(UUID orderId);
    Optional<Payment> findByProviderPaymentIntentId(String providerPaymentIntentId);
}
