package com.scaler.productcatalog.exception;

import org.springframework.http.HttpStatus;

/** Base type for domain errors that map to a stable API error code and HTTP status. */
public abstract class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    protected ApiException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
