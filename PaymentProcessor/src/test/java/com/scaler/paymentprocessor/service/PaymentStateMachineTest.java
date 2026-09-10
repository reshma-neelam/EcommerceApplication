package com.scaler.paymentprocessor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scaler.paymentprocessor.enums.PaymentStatus;
import com.scaler.paymentprocessor.exception.ConflictException;
import org.junit.jupiter.api.Test;

class PaymentStateMachineTest {

    @Test
    void validEdges() {
        assertThat(PaymentStateMachine.canTransition(PaymentStatus.INITIATED, PaymentStatus.SUCCEEDED)).isTrue();
        assertThat(PaymentStateMachine.canTransition(PaymentStatus.INITIATED, PaymentStatus.FAILED)).isTrue();
        assertThat(PaymentStateMachine.canTransition(PaymentStatus.FAILED, PaymentStatus.SUCCEEDED)).isTrue();
    }

    @Test
    void illegalEdgeThrows() {
        assertThatThrownBy(() ->
                PaymentStateMachine.assertCanTransition(PaymentStatus.SUCCEEDED, PaymentStatus.FAILED))
                .isInstanceOf(ConflictException.class);
    }
}
