package com.scaler.orderprocessor.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.orderprocessor.config.SecurityConfig;
import com.scaler.orderprocessor.dto.order.AddressDTO;
import com.scaler.orderprocessor.dto.order.OrderCreateRequestDTO;
import com.scaler.orderprocessor.dto.order.OrderResponseDTO;
import com.scaler.orderprocessor.exception.ConflictException;
import com.scaler.orderprocessor.security.AuthenticatedUser;
import com.scaler.orderprocessor.security.JwtAuthenticationFilter;
import com.scaler.orderprocessor.security.TokenService;
import com.scaler.orderprocessor.service.OrderService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private TokenService tokenService;

    private final UUID userId = UUID.randomUUID();

    private RequestPostProcessor customer() {
        return authentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId), null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    private OrderCreateRequestDTO validRequest() {
        AddressDTO address = AddressDTO.builder()
                .recipientName("Jane").line1("1 St").city("Town").postalCode("12345")
                .countryCode("IN").build();
        return OrderCreateRequestDTO.builder()
                .lines(List.of(OrderCreateRequestDTO.Line.builder()
                        .productId(UUID.randomUUID()).quantity(1).build()))
                .shippingAddress(address)
                .build();
    }

    @Test
    void create_withoutIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .with(customer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_withEmptyLines_returns400() throws Exception {
        OrderCreateRequestDTO request = validRequest();
        request.setLines(List.of());
        mockMvc.perform(post("/api/v1/orders")
                        .with(customer())
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void create_asCustomer_returns201WithLocation() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.create(eq(userId), eq("key-1"), any()))
                .thenReturn(OrderResponseDTO.builder().id(orderId).build());

        mockMvc.perform(post("/api/v1/orders")
                        .with(customer())
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(orderId.toString())))
                .andExpect(jsonPath("$.id").value(orderId.toString()));
    }

    @Test
    void create_whenServiceThrowsDuplicate_returns409() throws Exception {
        when(orderService.create(any(), any(), any()))
                .thenThrow(new ConflictException("DUPLICATE_REQUEST", "already used"));

        mockMvc.perform(post("/api/v1/orders")
                        .with(customer())
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_REQUEST"));
    }
}
