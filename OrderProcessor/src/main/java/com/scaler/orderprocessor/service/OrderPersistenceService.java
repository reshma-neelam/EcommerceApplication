package com.scaler.orderprocessor.service;

import com.scaler.orderprocessor.config.OrderPricingProperties;
import com.scaler.orderprocessor.dto.order.AddressDTO;
import com.scaler.orderprocessor.dto.order.OrderCreateRequestDTO;
import com.scaler.orderprocessor.dto.order.OrderResponseDTO;
import com.scaler.orderprocessor.enums.AddressType;
import com.scaler.orderprocessor.enums.ChangedByType;
import com.scaler.orderprocessor.enums.OrderStatus;
import com.scaler.orderprocessor.mapper.OrderMapper;
import com.scaler.orderprocessor.model.Order;
import com.scaler.orderprocessor.model.OrderAddress;
import com.scaler.orderprocessor.model.OrderIdempotency;
import com.scaler.orderprocessor.model.OrderItem;
import com.scaler.orderprocessor.model.OrderStatusHistory;
import com.scaler.orderprocessor.repository.OrderAddressRepository;
import com.scaler.orderprocessor.repository.OrderIdempotencyRepository;
import com.scaler.orderprocessor.repository.OrderItemRepository;
import com.scaler.orderprocessor.repository.OrderStatusHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists the order aggregate and its outbox events inside a single transaction.
 * Split from {@link OrderService} so the transactional boundary is applied via the
 * Spring proxy (avoids self-invocation) after the external reservation succeeds.
 */
@Service
@RequiredArgsConstructor
public class OrderPersistenceService {

    private final OrderItemRepository orderItemRepository;
    private final OrderAddressRepository orderAddressRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderIdempotencyRepository idempotencyRepository;
    private final OutboxWriter outboxWriter;
    private final OrderPricingProperties pricing;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public OrderResponseDTO persist(UUID orderId, UUID userId, String idempotencyKey, String requestHash,
                                    OrderCreateRequestDTO request, List<ResolvedLine> resolved,
                                    String currency, BigDecimal subtotal, BigDecimal total,
                                    long idempotencyRetentionHours) {
        Order order = new Order();
        order.setId(orderId);
        order.setOrderNumber(generateOrderNumber());
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCurrency(currency);
        order.setSubtotalAmount(subtotal.setScale(4, RoundingMode.HALF_UP));
        order.setDiscountAmount(pricing.getDiscountAmount().setScale(4, RoundingMode.HALF_UP));
        order.setTaxAmount(pricing.getTaxAmount().setScale(4, RoundingMode.HALF_UP));
        order.setShippingAmount(pricing.getShippingAmount().setScale(4, RoundingMode.HALF_UP));
        order.setTotalAmount(total);
        entityManager.persist(order);

        List<OrderItem> items = new ArrayList<>();
        for (ResolvedLine l : resolved) {
            OrderItem item = new OrderItem();
            item.setOrderId(orderId);
            item.setProductId(l.getProductId());
            item.setSkuSnapshot(l.getSku());
            item.setNameSnapshot(l.getName());
            item.setUnitPrice(l.getUnitPrice());
            item.setQuantity(l.getQuantity());
            item.setLineTotal(l.getLineTotal());
            item.setCurrency(currency);
            items.add(orderItemRepository.save(item));
        }

        List<OrderAddress> addresses = new ArrayList<>();
        addresses.add(saveAddress(orderId, AddressType.SHIPPING, request.getShippingAddress()));
        AddressDTO billing = request.getBillingAddress() != null
                ? request.getBillingAddress() : request.getShippingAddress();
        addresses.add(saveAddress(orderId, AddressType.BILLING, billing));

        appendHistory(orderId, null, OrderStatus.PENDING_PAYMENT, "ORDER_CREATED",
                ChangedByType.USER, userId.toString());

        OrderIdempotency record = new OrderIdempotency();
        record.setUserId(userId);
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setOrderId(orderId);
        record.setExpiresAt(Instant.now().plus(idempotencyRetentionHours, ChronoUnit.HOURS));
        idempotencyRepository.save(record);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId.toString());
        payload.put("userId", userId.toString());
        payload.put("status", OrderStatus.PENDING_PAYMENT.name());
        payload.put("currency", currency);
        payload.put("totalAmount", total.toPlainString());
        outboxWriter.write(orderId, "OrderCreated.v1", payload);
        outboxWriter.write(orderId, "OrderStatusChanged.v1", payload);

        return OrderMapper.toOrderDto(order, items, addresses);
    }

    private OrderAddress saveAddress(UUID orderId, AddressType type, AddressDTO dto) {
        OrderAddress a = new OrderAddress();
        a.setOrderId(orderId);
        a.setAddressType(type);
        a.setRecipientName(dto.getRecipientName());
        a.setLine1(dto.getLine1());
        a.setLine2(dto.getLine2());
        a.setCity(dto.getCity());
        a.setStateRegion(dto.getStateRegion());
        a.setPostalCode(dto.getPostalCode());
        a.setCountryCode(dto.getCountryCode());
        a.setPhone(dto.getPhone());
        return orderAddressRepository.save(a);
    }

    private void appendHistory(UUID orderId, OrderStatus from, OrderStatus to, String reasonCode,
                               ChangedByType by, String byId) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrderId(orderId);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setReasonCode(reasonCode);
        h.setChangedByType(by);
        h.setChangedById(byId);
        historyRepository.save(h);
    }

    private String generateOrderNumber() {
        return "ORD-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase()
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
