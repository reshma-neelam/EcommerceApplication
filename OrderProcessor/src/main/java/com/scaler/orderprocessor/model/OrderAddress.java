package com.scaler.orderprocessor.model;

import com.scaler.orderprocessor.enums.AddressType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "order_address")
public class OrderAddress {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(36)", updatable = false, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "order_id", columnDefinition = "char(36)", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 24)
    private AddressType addressType;

    @Column(name = "recipient_name", nullable = false, length = 200)
    private String recipientName;

    @Column(nullable = false, length = 255)
    private String line1;

    @Column(length = 255)
    private String line2;

    @Column(nullable = false, length = 128)
    private String city;

    @Column(name = "state_region", length = 128)
    private String stateRegion;

    @Column(name = "postal_code", nullable = false, length = 32)
    private String postalCode;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(length = 32)
    private String phone;
}
