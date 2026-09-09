package com.scaler.orderprocessor.model;

import com.scaler.orderprocessor.enums.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order {

    // Assigned by the service up-front so it can key the inventory reservation before persistence.
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(36)", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "order_number", nullable = false, length = 32, updatable = false)
    private String orderNumber;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", columnDefinition = "char(36)", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "shipping_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "latest_payment_id", columnDefinition = "char(36)")
    private UUID latestPaymentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
