package com.scaler.orderprocessor.mapper;

import com.scaler.orderprocessor.dto.order.AddressDTO;
import com.scaler.orderprocessor.dto.order.OrderItemResponseDTO;
import com.scaler.orderprocessor.dto.order.OrderResponseDTO;
import com.scaler.orderprocessor.enums.AddressType;
import com.scaler.orderprocessor.model.Order;
import com.scaler.orderprocessor.model.OrderAddress;
import com.scaler.orderprocessor.model.OrderItem;
import java.util.List;

public final class OrderMapper {

    private OrderMapper() {}

    public static OrderItemResponseDTO toItemDto(OrderItem i) {
        return OrderItemResponseDTO.builder()
                .productId(i.getProductId())
                .sku(i.getSkuSnapshot())
                .name(i.getNameSnapshot())
                .unitPrice(i.getUnitPrice())
                .quantity(i.getQuantity())
                .lineTotal(i.getLineTotal())
                .currency(i.getCurrency())
                .build();
    }

    public static AddressDTO toAddressDto(OrderAddress a) {
        return AddressDTO.builder()
                .recipientName(a.getRecipientName())
                .line1(a.getLine1())
                .line2(a.getLine2())
                .city(a.getCity())
                .stateRegion(a.getStateRegion())
                .postalCode(a.getPostalCode())
                .countryCode(a.getCountryCode())
                .phone(a.getPhone())
                .build();
    }

    public static OrderResponseDTO toOrderDto(Order o, List<OrderItem> items, List<OrderAddress> addresses) {
        AddressDTO shipping = addresses.stream()
                .filter(a -> a.getAddressType() == AddressType.SHIPPING)
                .findFirst().map(OrderMapper::toAddressDto).orElse(null);
        AddressDTO billing = addresses.stream()
                .filter(a -> a.getAddressType() == AddressType.BILLING)
                .findFirst().map(OrderMapper::toAddressDto).orElse(null);
        return OrderResponseDTO.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .userId(o.getUserId())
                .status(o.getStatus())
                .currency(o.getCurrency())
                .subtotalAmount(o.getSubtotalAmount())
                .discountAmount(o.getDiscountAmount())
                .taxAmount(o.getTaxAmount())
                .shippingAmount(o.getShippingAmount())
                .totalAmount(o.getTotalAmount())
                .items(items.stream().map(OrderMapper::toItemDto).toList())
                .shippingAddress(shipping)
                .billingAddress(billing)
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
