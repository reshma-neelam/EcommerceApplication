package com.scaler.orderprocessor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scaler.orderprocessor.client.ProductClient;
import com.scaler.orderprocessor.client.ProductSnapshotDTO;
import com.scaler.orderprocessor.config.OrderPricingProperties;
import com.scaler.orderprocessor.dto.order.AddressDTO;
import com.scaler.orderprocessor.dto.order.OrderCreateRequestDTO;
import com.scaler.orderprocessor.dto.order.OrderResponseDTO;
import com.scaler.orderprocessor.repository.OrderIdempotencyRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceMoneyTest {

    @Mock
    private ProductClient productClient;
    @Mock
    private RequestHasher requestHasher;
    @Mock
    private OrderIdempotencyRepository idempotencyRepository;
    @Mock
    private OrderPersistenceService orderPersistenceService;
    @Spy
    private OrderPricingProperties pricing = new OrderPricingProperties();

    @InjectMocks
    private OrderService orderService;

    private final UUID userId = UUID.randomUUID();
    private final UUID productA = UUID.randomUUID();
    private final UUID productB = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(requestHasher.hash(any())).thenReturn("hash");
        when(idempotencyRepository.findByUserIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
    }

    private OrderCreateRequestDTO request() {
        AddressDTO address = AddressDTO.builder()
                .recipientName("Jane").line1("1 St").city("Town").postalCode("12345")
                .countryCode("IN").build();
        return OrderCreateRequestDTO.builder()
                .lines(List.of(
                        OrderCreateRequestDTO.Line.builder().productId(productA).quantity(2).build(),
                        OrderCreateRequestDTO.Line.builder().productId(productB).quantity(1).build()))
                .shippingAddress(address)
                .build();
    }

    private ProductSnapshotDTO snapshot(UUID id, String price) {
        ProductSnapshotDTO p = new ProductSnapshotDTO();
        p.setId(id);
        p.setSku("SKU-" + id.toString().substring(0, 4));
        p.setName("Product " + id.toString().substring(0, 4));
        p.setStatus("ACTIVE");
        p.setBasePrice(new BigDecimal(price));
        p.setCurrency("INR");
        p.setAvailableQuantity(100);
        return p;
    }

    @Test
    void create_computesSubtotalAndTotal_withFourScale() {
        when(productClient.getSnapshot(productA)).thenReturn(snapshot(productA, "100.00"));
        when(productClient.getSnapshot(productB)).thenReturn(snapshot(productB, "50.50"));
        when(orderPersistenceService.persist(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyLong()))
                .thenReturn(OrderResponseDTO.builder().id(UUID.randomUUID()).build());

        orderService.create(userId, "key-1", request());

        ArgumentCaptor<BigDecimal> subtotal = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> total = ArgumentCaptor.forClass(BigDecimal.class);
        verify(orderPersistenceService).persist(any(), eq(userId), eq("key-1"), eq("hash"), any(), any(),
                eq("INR"), subtotal.capture(), total.capture(), anyLong());

        assertThat(subtotal.getValue()).isEqualByComparingTo("250.5000");
        assertThat(total.getValue()).isEqualByComparingTo("250.5000");
        assertThat(total.getValue().scale()).isEqualTo(4);
    }

    @Test
    void create_reservesBeforePersist_andReleasesOnPersistFailure() {
        when(productClient.getSnapshot(productA)).thenReturn(snapshot(productA, "100.00"));
        when(productClient.getSnapshot(productB)).thenReturn(snapshot(productB, "50.50"));
        when(orderPersistenceService.persist(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyLong()))
                .thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> orderService.create(userId, "key-2", request()))
                .isInstanceOf(RuntimeException.class);

        ArgumentCaptor<UUID> orderId = ArgumentCaptor.forClass(UUID.class);
        verify(productClient).reserve(orderId.capture(), any());
        verify(productClient, times(1)).releaseQuietly(orderId.getValue());
    }
}
