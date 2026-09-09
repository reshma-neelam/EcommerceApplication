package com.scaler.orderprocessor.repository;

import com.scaler.orderprocessor.model.OrderStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
