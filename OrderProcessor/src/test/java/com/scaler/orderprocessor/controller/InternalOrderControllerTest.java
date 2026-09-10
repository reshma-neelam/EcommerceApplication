package com.scaler.orderprocessor.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.scaler.orderprocessor.config.SecurityConfig;
import com.scaler.orderprocessor.dto.order.PaymentDetailsResponseDTO;
import com.scaler.orderprocessor.security.JwtAuthenticationFilter;
import com.scaler.orderprocessor.security.TokenService;
import com.scaler.orderprocessor.service.OrderService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InternalOrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class InternalOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private TokenService tokenService;

    @Test
    void paymentDetails_noAuth_returnsPayableProjection() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(orderService.getPaymentDetails(orderId)).thenReturn(PaymentDetailsResponseDTO.builder()
                .orderId(orderId).userId(userId).currency("INR")
                .totalAmount(new BigDecimal("120.0000")).payable(true).build());

        mockMvc.perform(get("/internal/v1/orders/{orderId}/payment-details", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.payable").value(true));
    }
}
