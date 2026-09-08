package com.scaler.usermanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scaler.usermanagement.dto.auth.LoginRequestDTO;
import com.scaler.usermanagement.dto.auth.SignupRequestDTO;
import com.scaler.usermanagement.exception.ConflictException;
import com.scaler.usermanagement.repository.UserCredentialRepository;
import com.scaler.usermanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthenticationServiceTest {

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserCredentialRepository credentialRepository;

    @Test
    void signup_hashesPassword_andNeverStoresPlaintext() {
        var user = authenticationService.signup(
                new SignupRequestDTO("New.User@Example.com", "Sup3rSecret!", "New", "User"));
        var credential = credentialRepository.findById(user.getId()).orElseThrow();
        assertThat(credential.getPasswordHash()).isNotEqualTo("Sup3rSecret!");
        assertThat(credential.getPasswordHash()).startsWith("$2");
        assertThat(user.getEmail()).isEqualTo("new.user@example.com");
        assertThat(user.getRoles()).contains("CUSTOMER");
    }

    @Test
    void signup_duplicateEmail_throwsConflict() {
        authenticationService.signup(new SignupRequestDTO("dupe@example.com", "Sup3rSecret!", "A", "B"));
        assertThatThrownBy(() -> authenticationService.signup(
                new SignupRequestDTO("DUPE@example.com", "Another1!", "C", "D")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void login_validCredentials_issuesTokens() {
        authenticationService.signup(new SignupRequestDTO("login@example.com", "Sup3rSecret!", "L", "G"));
        var tokens = authenticationService.login(new LoginRequestDTO("login@example.com", "Sup3rSecret!"));
        assertThat(tokens.getAccessToken()).isNotBlank();
        assertThat(tokens.getRefreshToken()).isNotBlank();
        assertThat(tokens.getTokenType()).isEqualTo("Bearer");
    }
}
