package com.scaler.usermanagement.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.scaler.usermanagement.config.SecurityConfig;
import com.scaler.usermanagement.dto.auth.UserResponseDTO;
import com.scaler.usermanagement.security.JwtAuthenticationFilter;
import com.scaler.usermanagement.security.TokenService;
import com.scaler.usermanagement.service.AddressService;
import com.scaler.usermanagement.service.UserProfileService;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
        com.scaler.usermanagement.security.JwtTokenService.class})
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TokenService tokenService;

    @MockitoBean
    UserProfileService userProfileService;

    @MockitoBean
    AddressService addressService;

    @Test
    void me_withValidJwt_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userProfileService.getCurrent(userId))
                .thenReturn(new UserResponseDTO(userId, "u@b.com", "U", "B", null, "ACTIVE",
                        Set.of("CUSTOMER"), Instant.now()));
        String token = tokenService.createAccessToken(userId, Set.of("CUSTOMER"));
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("u@b.com"));
    }

    @Test
    void me_withInvalidJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
