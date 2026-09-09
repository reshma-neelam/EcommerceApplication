package com.scaler.orderprocessor.repository;

import com.scaler.orderprocessor.model.OrderAddress;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAddressRepository extends JpaRepository<OrderAddress, UUID> {
    List<OrderAddress> findByOrderId(UUID orderId);
}
