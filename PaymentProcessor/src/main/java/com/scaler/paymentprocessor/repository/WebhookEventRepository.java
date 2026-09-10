package com.scaler.paymentprocessor.repository;

import com.scaler.paymentprocessor.model.WebhookEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    Optional<WebhookEvent> findByProviderEventId(String providerEventId);
}
