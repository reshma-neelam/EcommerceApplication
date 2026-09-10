package com.scaler.paymentprocessor.service;

import com.scaler.paymentprocessor.enums.PaymentStatus;
import com.scaler.paymentprocessor.exception.ConflictException;
import java.util.Map;
import java.util.Set;

public final class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = Map.of(
            PaymentStatus.INITIATED, Set.of(PaymentStatus.SUCCEEDED, PaymentStatus.FAILED),
            PaymentStatus.FAILED, Set.of(PaymentStatus.SUCCEEDED),
            PaymentStatus.SUCCEEDED, Set.of());

    private PaymentStateMachine() {}

    public static boolean canTransition(PaymentStatus from, PaymentStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static void assertCanTransition(PaymentStatus from, PaymentStatus to) {
        if (!canTransition(from, to)) {
            throw new ConflictException("INVALID_PAYMENT_STATE",
                    "Illegal payment transition " + from + " -> " + to);
        }
    }
}
