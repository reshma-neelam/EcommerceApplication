package com.scaler.usermanagement.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.scaler.usermanagement.dto.auth.AuthTokensDTO;
import com.scaler.usermanagement.dto.auth.LoginRequestDTO;
import com.scaler.usermanagement.dto.auth.LogoutRequestDTO;
import com.scaler.usermanagement.dto.auth.RefreshRequestDTO;
import com.scaler.usermanagement.dto.auth.SignupRequestDTO;
import com.scaler.usermanagement.service.AuthenticationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    AuthenticationService authenticationService;

    @Test
    void refreshRotation_revokesOldToken_andLogoutRevokesSession() {
        authenticationService.signup(new SignupRequestDTO("flow@example.com", "Sup3rSecret!", "F", "L"));
        AuthTokensDTO tokens = authenticationService.login(new LoginRequestDTO("flow@example.com", "Sup3rSecret!"));

        AuthTokensDTO rotated = authenticationService.refresh(new RefreshRequestDTO(tokens.getRefreshToken()));
        assertThat(rotated.getRefreshToken()).isNotEqualTo(tokens.getRefreshToken());

        // Old refresh token no longer valid
        Assertions.assertThrows(RuntimeException.class,
                () -> authenticationService.refresh(new RefreshRequestDTO(tokens.getRefreshToken())));

        // Logout revokes the rotated session; subsequent refresh fails
        authenticationService.logout(new LogoutRequestDTO(rotated.getRefreshToken()));
        Assertions.assertThrows(RuntimeException.class,
                () -> authenticationService.refresh(new RefreshRequestDTO(rotated.getRefreshToken())));
    }
}
