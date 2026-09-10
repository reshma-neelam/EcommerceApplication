package com.scaler.paymentprocessor.security;

import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

public interface TokenService {
    ParsedToken parse(String token);

    @Getter
    @AllArgsConstructor
    class ParsedToken {
        private final UUID userId;
        private final Set<String> roles;
    }
}
