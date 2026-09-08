package com.scaler.usermanagement.security;

import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

public interface TokenService {
    String createAccessToken(UUID userId, Set<String> roles);

    ParsedToken parse(String token);

    @Getter
    @AllArgsConstructor
    class ParsedToken {
        private final UUID userId;
        private final Set<String> roles;
    }
}
