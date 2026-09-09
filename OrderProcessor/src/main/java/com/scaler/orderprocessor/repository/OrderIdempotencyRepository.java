package com.scaler.orderprocessor.repository;

import com.scaler.orderprocessor.model.OrderIdempotency;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderIdempotencyRepository extends JpaRepository<OrderIdempotency, UUID> {
    Optional<OrderIdempotency> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
