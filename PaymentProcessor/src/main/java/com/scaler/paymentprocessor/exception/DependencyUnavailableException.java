package com.scaler.paymentprocessor.exception;

import org.springframework.http.HttpStatus;

public class DependencyUnavailableException extends ApiException {
    public DependencyUnavailableException(String code, String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, code, message);
    }
}
