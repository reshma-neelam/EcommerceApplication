package com.scaler.usermanagement.controller;

import com.scaler.usermanagement.dto.auth.AuthTokensDTO;
import com.scaler.usermanagement.dto.auth.LoginRequestDTO;
import com.scaler.usermanagement.dto.auth.LogoutRequestDTO;
import com.scaler.usermanagement.dto.auth.RefreshRequestDTO;
import com.scaler.usermanagement.dto.auth.SignupRequestDTO;
import com.scaler.usermanagement.dto.auth.UserResponseDTO;
import com.scaler.usermanagement.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO signup(@Valid @RequestBody SignupRequestDTO request) {
        return authenticationService.signup(request);
    }

    @PostMapping("/login")
    public AuthTokensDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return authenticationService.login(request);
    }

    @PostMapping("/refresh")
    public AuthTokensDTO refresh(@Valid @RequestBody RefreshRequestDTO request) {
        return authenticationService.refresh(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequestDTO request) {
        authenticationService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
