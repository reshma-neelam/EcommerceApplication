package com.scaler.paymentprocessor.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.scaler.paymentprocessor.config.SecurityConfig;
import com.scaler.paymentprocessor.dto.payment.PaymentResponseDTO;
import com.scaler.paymentprocessor.enums.PaymentStatus;
import com.scaler.paymentprocessor.security.AuthenticatedUser;
import com.scaler.paymentprocessor.security.JwtAuthenticationFilter;
import com.scaler.paymentprocessor.security.TokenService;
import com.scaler.paymentprocessor.service.PaymentService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private TokenService tokenService;

    private final UUID userId = UUID.randomUUID();

    private RequestPostProcessor customer() {
        return authentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId), null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    @Test
    void get_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void get_asOwner_returnsPayment() throws Exception {
        UUID paymentId = UUID.randomUUID();
        when(paymentService.getForUser(paymentId, userId, false)).thenReturn(PaymentResponseDTO.builder()
                .id(paymentId).orderId(UUID.randomUUID()).status(PaymentStatus.INITIATED)
                .currency("INR").amount(new BigDecimal("120.0000")).build());

        mockMvc.perform(get("/api/v1/payments/{id}", paymentId).with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }
}
