package com.scaler.orderprocessor.service;

import com.scaler.orderprocessor.enums.OrderStatus;
import com.scaler.orderprocessor.exception.ConflictException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(OrderStatus.PENDING_PAYMENT,
                Set.of(OrderStatus.CONFIRMED, OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.CONFIRMED,
                Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLATION_PENDING));
        ALLOWED.put(OrderStatus.PROCESSING,
                Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLATION_PENDING));
        ALLOWED.put(OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED));
        ALLOWED.put(OrderStatus.DELIVERED, Set.of());
        ALLOWED.put(OrderStatus.PAYMENT_FAILED,
                Set.of(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.CANCELLATION_PENDING, Set.of(OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.CANCELLED, Set.of());
    }

    private OrderStateMachine() {
    }

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static void assertCanTransition(OrderStatus from, OrderStatus to) {
        if (!canTransition(from, to)) {
            throw new ConflictException("INVALID_ORDER_STATE",
                    "Illegal order transition: " + from + " -> " + to);
        }
    }
}
