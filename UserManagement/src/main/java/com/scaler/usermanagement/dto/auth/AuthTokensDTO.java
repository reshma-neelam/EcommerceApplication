package com.scaler.usermanagement.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthTokensDTO {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresInSeconds;
}
