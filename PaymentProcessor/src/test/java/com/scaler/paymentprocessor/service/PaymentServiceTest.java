package com.scaler.paymentprocessor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scaler.paymentprocessor.client.OrderPaymentDetailsClient;
import com.scaler.paymentprocessor.client.OrderPaymentDetailsDTO;
import com.scaler.paymentprocessor.dto.payment.PaymentResponseDTO;
import com.scaler.paymentprocessor.enums.PaymentStatus;
import com.scaler.paymentprocessor.exception.BadRequestException;
import com.scaler.paymentprocessor.exception.ConflictException;
import com.scaler.paymentprocessor.exception.NotFoundException;
import com.scaler.paymentprocessor.gateway.CreatePaymentResult;
import com.scaler.paymentprocessor.gateway.PaymentGateway;
import com.scaler.paymentprocessor.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private OrderPaymentDetailsClient orderClient;
    @MockitoBean
    private PaymentGateway paymentGateway;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void clean() {
        paymentRepository.deleteAll();
    }

    private OrderPaymentDetailsDTO payableOrder(UUID orderId) {
        OrderPaymentDetailsDTO dto = new OrderPaymentDetailsDTO();
        dto.setOrderId(orderId);
        dto.setUserId(userId);
        dto.setCurrency("INR");
        dto.setTotalAmount(new BigDecimal("120.0000"));
        dto.setPayable(true);
        return dto;
    }

    private void stubGateway() {
        when(paymentGateway.createPayment(any())).thenReturn(CreatePaymentResult.builder()
                .providerPaymentIntentId("pi_test_1")
                .clientSecret("pi_test_1_secret_abc")
                .providerStatus("requires_payment_method")
                .build());
    }

    @Test
    void create_success_persistsInitiatedAndReturnsClientSecret() {
        UUID orderId = UUID.randomUUID();
        when(orderClient.fetch(orderId)).thenReturn(payableOrder(orderId));
        stubGateway();

        PaymentResponseDTO response = paymentService.create(userId, "key-1", orderId);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(response.getClientSecret()).isEqualTo("pi_test_1_secret_abc");
        assertThat(paymentRepository.findByOrderId(orderId)).isPresent()
                .get().satisfies(p -> {
                    assertThat(p.getProviderPaymentIntentId()).isEqualTo("pi_test_1");
                    assertThat(p.getAmount()).isEqualByComparingTo("120.0000");
                });
    }

    @Test
    void create_replaySameKey_returnsSamePaymentAndCallsGatewayOnce() {
        UUID orderId = UUID.randomUUID();
        when(orderClient.fetch(orderId)).thenReturn(payableOrder(orderId));
        stubGateway();

        PaymentResponseDTO first = paymentService.create(userId, "key-1", orderId);
        PaymentResponseDTO replay = paymentService.create(userId, "key-1", orderId);

        assertThat(replay.getId()).isEqualTo(first.getId());
        assertThat(paymentRepository.count()).isEqualTo(1);
        verify(paymentGateway, times(1)).createPayment(any());
    }

    @Test
    void create_orderNotPayable_throwsConflict() {
        UUID orderId = UUID.randomUUID();
        OrderPaymentDetailsDTO notPayable = payableOrder(orderId);
        notPayable.setPayable(false);
        when(orderClient.fetch(orderId)).thenReturn(notPayable);

        assertThatThrownBy(() -> paymentService.create(userId, "key-1", orderId))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code", "ORDER_NOT_PAYABLE");
    }

    @Test
    void create_wrongOwner_throwsNotFound() {
        UUID orderId = UUID.randomUUID();
        OrderPaymentDetailsDTO otherOwner = payableOrder(orderId);
        otherOwner.setUserId(UUID.randomUUID());
        when(orderClient.fetch(orderId)).thenReturn(otherOwner);

        assertThatThrownBy(() -> paymentService.create(userId, "key-1", orderId))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", "ORDER_NOT_FOUND");
    }

    @Test
    void create_mismatchedCurrency_throwsBadRequest() {
        UUID orderId = UUID.randomUUID();
        OrderPaymentDetailsDTO usd = payableOrder(orderId);
        usd.setCurrency("USD");
        when(orderClient.fetch(orderId)).thenReturn(usd);

        assertThatThrownBy(() -> paymentService.create(userId, "key-1", orderId))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", "UNSUPPORTED_CURRENCY");
    }
}
