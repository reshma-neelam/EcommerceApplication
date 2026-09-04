package com.scaler.productcatalog.repository;

import com.scaler.productcatalog.enums.OutboxStatus;
import com.scaler.productcatalog.model.OutboxEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
