package com.scaler.orderprocessor.model;

import com.scaler.orderprocessor.enums.ChangedByType;
import com.scaler.orderprocessor.enums.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "order_status_history")
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "order_id", columnDefinition = "char(36)", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 40)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 40)
    private OrderStatus toStatus;

    @Column(name = "reason_code", length = 64)
    private String reasonCode;

    @Column(name = "reason_text", length = 500)
    private String reasonText;

    @Enumerated(EnumType.STRING)
    @Column(name = "changed_by_type", nullable = false, length = 24)
    private ChangedByType changedByType = ChangedByType.SYSTEM;

    @Column(name = "changed_by_id", length = 128)
    private String changedById;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
