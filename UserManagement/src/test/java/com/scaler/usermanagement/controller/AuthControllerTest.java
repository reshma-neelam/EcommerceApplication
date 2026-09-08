package com.scaler.usermanagement.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.scaler.usermanagement.config.SecurityConfig;
import com.scaler.usermanagement.dto.auth.UserResponseDTO;
import com.scaler.usermanagement.security.JwtAuthenticationFilter;
import com.scaler.usermanagement.security.JwtTokenService;
import com.scaler.usermanagement.service.AuthenticationService;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenService.class})
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthenticationService authenticationService;

    @Test
    void signup_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(authenticationService.signup(ArgumentMatchers.any()))
                .thenReturn(new UserResponseDTO(id, "a@b.com", "A", "B", null, "ACTIVE",
                        Set.of("CUSTOMER"), Instant.now()));
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content("{\"email\":\"a@b.com\",\"password\":\"Sup3rSecret!\","
                                + "\"firstName\":\"A\",\"lastName\":\"B\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("a@b.com"));
    }

    @Test
    void signup_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content("{\"email\":\"not-an-email\",\"password\":\"x\","
                                + "\"firstName\":\"\",\"lastName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
