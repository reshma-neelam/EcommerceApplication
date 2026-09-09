package com.scaler.orderprocessor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scaler.orderprocessor.enums.OrderStatus;
import com.scaler.orderprocessor.exception.ConflictException;
import org.junit.jupiter.api.Test;

class OrderStateMachineTest {

    @Test
    void canTransition_validEdges_returnTrue() {
        assertThat(OrderStateMachine.canTransition(OrderStatus.PENDING_PAYMENT, OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStateMachine.canTransition(OrderStatus.CONFIRMED, OrderStatus.PROCESSING)).isTrue();
        assertThat(OrderStateMachine.canTransition(OrderStatus.SHIPPED, OrderStatus.DELIVERED)).isTrue();
    }

    @Test
    void canTransition_illegalEdges_returnFalse() {
        assertThat(OrderStateMachine.canTransition(OrderStatus.DELIVERED, OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStateMachine.canTransition(OrderStatus.CANCELLED, OrderStatus.CONFIRMED)).isFalse();
    }

    @Test
    void assertCanTransition_whenIllegal_throwsInvalidOrderState() {
        assertThatThrownBy(() ->
                OrderStateMachine.assertCanTransition(OrderStatus.DELIVERED, OrderStatus.PENDING_PAYMENT))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Illegal order transition");
    }
}
