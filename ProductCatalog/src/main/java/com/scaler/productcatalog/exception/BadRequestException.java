package com.scaler.productcatalog.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(String code, String message) {
        super(code, HttpStatus.BAD_REQUEST, message);
    }
}
