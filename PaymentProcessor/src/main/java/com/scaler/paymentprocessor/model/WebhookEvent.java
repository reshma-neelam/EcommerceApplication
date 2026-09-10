package com.scaler.paymentprocessor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "webhook_event")
public class WebhookEvent {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(36)", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "provider_event_id", nullable = false, length = 120, updatable = false)
    private String providerEventId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(nullable = false, length = 40)
    private String status;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "payment_id", columnDefinition = "char(36)")
    private UUID paymentId;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @PrePersist
    void onCreate() {
        this.receivedAt = Instant.now();
    }
}
