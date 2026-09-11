package com.scaler.orderprocessor.repository;

import com.scaler.orderprocessor.model.InboxEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {
}
