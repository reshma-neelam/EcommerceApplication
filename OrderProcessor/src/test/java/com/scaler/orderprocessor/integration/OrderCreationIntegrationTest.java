package com.scaler.orderprocessor.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.scaler.orderprocessor.client.ProductClient;
import com.scaler.orderprocessor.client.ProductSnapshotDTO;
import com.scaler.orderprocessor.dto.order.AddressDTO;
import com.scaler.orderprocessor.dto.order.OrderCreateRequestDTO;
import com.scaler.orderprocessor.dto.order.OrderResponseDTO;
import com.scaler.orderprocessor.enums.OrderStatus;
import com.scaler.orderprocessor.exception.ConflictException;
import com.scaler.orderprocessor.exception.NotFoundException;
import com.scaler.orderprocessor.repository.OrderAddressRepository;
import com.scaler.orderprocessor.repository.OrderIdempotencyRepository;
import com.scaler.orderprocessor.repository.OrderItemRepository;
import com.scaler.orderprocessor.repository.OrderRepository;
import com.scaler.orderprocessor.repository.OrderStatusHistoryRepository;
import com.scaler.orderprocessor.repository.OutboxEventRepository;
import com.scaler.orderprocessor.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class OrderCreationIntegrationTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private OrderAddressRepository orderAddressRepository;
    @Autowired
    private OrderStatusHistoryRepository historyRepository;
    @Autowired
    private OrderIdempotencyRepository idempotencyRepository;
    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private ProductClient productClient;

    private final UUID userId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void cleanAndStub() {
        historyRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderAddressRepository.deleteAll();
        idempotencyRepository.deleteAll();
        outboxEventRepository.deleteAll();
        orderRepository.deleteAll();

        ProductSnapshotDTO snapshot = new ProductSnapshotDTO();
        snapshot.setId(productId);
        snapshot.setSku("SKU-1");
        snapshot.setName("Widget");
        snapshot.setStatus("ACTIVE");
        snapshot.setBasePrice(new BigDecimal("100.0000"));
        snapshot.setCurrency("INR");
        snapshot.setAvailableQuantity(50);
        when(productClient.getSnapshot(productId)).thenReturn(snapshot);
    }

    private OrderCreateRequestDTO request(int quantity) {
        AddressDTO address = AddressDTO.builder()
                .recipientName("Jane").line1("1 St").city("Town").postalCode("12345")
                .countryCode("IN").build();
        return OrderCreateRequestDTO.builder()
                .lines(List.of(OrderCreateRequestDTO.Line.builder()
                        .productId(productId).quantity(quantity).build()))
                .shippingAddress(address)
                .build();
    }

    @Test
    void create_persistsOrderItemsAddressesHistoryAndOutbox() {
        OrderResponseDTO created = orderService.create(userId, "key-1", request(2));

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(created.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(created.getSubtotalAmount()).isEqualByComparingTo("200.0000");
        assertThat(created.getTotalAmount()).isEqualByComparingTo("200.0000");
        assertThat(orderItemRepository.findByOrderId(created.getId())).hasSize(1);
        assertThat(orderAddressRepository.findByOrderId(created.getId())).hasSize(2);
        assertThat(historyRepository.findByOrderIdOrderByCreatedAtAsc(created.getId()))
                .hasSize(1)
                .allSatisfy(h -> assertThat(h.getToStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT));
        assertThat(outboxEventRepository.count()).isEqualTo(2);
    }

    @Test
    void create_sameKeySameBody_replaysWithoutDuplicate() {
        OrderResponseDTO first = orderService.create(userId, "key-1", request(2));
        OrderResponseDTO replay = orderService.create(userId, "key-1", request(2));

        assertThat(replay.getId()).isEqualTo(first.getId());
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void create_sameKeyDifferentBody_throwsDuplicateRequest() {
        orderService.create(userId, "key-1", request(2));

        assertThatThrownBy(() -> orderService.create(userId, "key-1", request(3)))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code", "DUPLICATE_REQUEST");
    }

    @Test
    void getForUser_nonOwnerNonAdmin_throwsResourceNotFound() {
        OrderResponseDTO created = orderService.create(userId, "key-1", request(2));
        UUID otherUser = UUID.randomUUID();

        assertThatThrownBy(() -> orderService.getForUser(created.getId(), otherUser, false))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
    }
}
