package com.scaler.usermanagement.observability;

import org.slf4j.MDC;

public final class CorrelationId {

    private CorrelationId() {
    }

    public static String current() {
        String value = MDC.get(CorrelationIdFilter.MDC_KEY);
        return value != null ? value : "unknown";
    }
}
