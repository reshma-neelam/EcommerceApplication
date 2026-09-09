package com.scaler.orderprocessor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(36)", updatable = false, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "order_id", columnDefinition = "char(36)", nullable = false)
    private UUID orderId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", columnDefinition = "char(36)", nullable = false)
    private UUID productId;

    @Column(name = "sku_snapshot", nullable = false, length = 64)
    private String skuSnapshot;

    @Column(name = "name_snapshot", nullable = false, length = 255)
    private String nameSnapshot;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTotal;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency;
}
