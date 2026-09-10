package com.scaler.paymentprocessor.security;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthenticatedUser {
    private final UUID userId;
}
