package com.scaler.orderprocessor.service;

import com.scaler.orderprocessor.client.ProductClient;
import com.scaler.orderprocessor.client.ProductSnapshotDTO;
import com.scaler.orderprocessor.client.ReserveRequest;
import com.scaler.orderprocessor.config.OrderPricingProperties;
import com.scaler.orderprocessor.dto.order.OrderCreateRequestDTO;
import com.scaler.orderprocessor.dto.order.OrderResponseDTO;
import com.scaler.orderprocessor.dto.order.OrderStatusResponseDTO;
import com.scaler.orderprocessor.dto.order.PaymentDetailsResponseDTO;
import com.scaler.orderprocessor.enums.OrderStatus;
import com.scaler.orderprocessor.exception.BadRequestException;
import com.scaler.orderprocessor.exception.ConflictException;
import com.scaler.orderprocessor.exception.NotFoundException;
import com.scaler.orderprocessor.mapper.OrderMapper;
import com.scaler.orderprocessor.model.Order;
import com.scaler.orderprocessor.model.OrderIdempotency;
import com.scaler.orderprocessor.repository.OrderAddressRepository;
import com.scaler.orderprocessor.repository.OrderIdempotencyRepository;
import com.scaler.orderprocessor.repository.OrderItemRepository;
import com.scaler.orderprocessor.repository.OrderRepository;
import com.scaler.orderprocessor.repository.OrderStatusHistoryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderAddressRepository orderAddressRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderIdempotencyRepository idempotencyRepository;
    private final ProductClient productClient;
    private final RequestHasher requestHasher;
    private final OrderPricingProperties pricing;
    private final OrderPersistenceService orderPersistenceService;

    @Value("${order.idempotency.retention-hours:24}")
    private long idempotencyRetentionHours;

    // ---------- Create ----------

    public OrderResponseDTO create(UUID userId, String idempotencyKey, OrderCreateRequestDTO request) {
        String requestHash = requestHasher.hash(request);

        Optional<OrderIdempotency> existing =
                idempotencyRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) {
            OrderIdempotency record = existing.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new ConflictException("DUPLICATE_REQUEST",
                        "Idempotency-Key already used with a different request body");
            }
            return getByIdInternal(record.getOrderId());
        }

        // Snapshot + validate products (single currency, active, computable line totals).
        List<ResolvedLine> resolved = resolveLines(request);
        String currency = pricing.getCurrency();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ResolvedLine line : resolved) {
            subtotal = subtotal.add(line.getLineTotal());
        }
        BigDecimal total = subtotal
                .subtract(pricing.getDiscountAmount())
                .add(pricing.getTaxAmount())
                .add(pricing.getShippingAmount())
                .setScale(4, RoundingMode.HALF_UP);

        UUID orderId = UUID.randomUUID();
        productClient.reserve(orderId, resolved.stream()
                .map(l -> ReserveRequest.Line.builder()
                        .productId(l.getProductId()).quantity(l.getQuantity()).build())
                .toList());

        try {
            return orderPersistenceService.persist(orderId, userId, idempotencyKey, requestHash, request,
                    resolved, currency, subtotal, total, idempotencyRetentionHours);
        } catch (RuntimeException ex) {
            productClient.releaseQuietly(orderId); // compensation
            throw ex;
        }
    }

    // ---------- Reads ----------

    @Transactional(readOnly = true)
    public OrderResponseDTO getForUser(UUID orderId, UUID userId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> isAdmin || o.getUserId().equals(userId))
                .orElseThrow(() -> new NotFoundException("RESOURCE_NOT_FOUND", "Order not found: " + orderId));
        return OrderMapper.toOrderDto(order,
                orderItemRepository.findByOrderId(orderId),
                orderAddressRepository.findByOrderId(orderId));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> listForUser(UUID userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(o -> OrderMapper.toOrderDto(o,
                        orderItemRepository.findByOrderId(o.getId()),
                        orderAddressRepository.findByOrderId(o.getId())));
    }

    @Transactional(readOnly = true)
    public OrderStatusResponseDTO getStatus(UUID orderId, UUID userId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> isAdmin || o.getUserId().equals(userId))
                .orElseThrow(() -> new NotFoundException("RESOURCE_NOT_FOUND", "Order not found: " + orderId));
        List<OrderStatusResponseDTO.HistoryEntry> history =
                historyRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                        .map(h -> OrderStatusResponseDTO.HistoryEntry.builder()
                                .fromStatus(h.getFromStatus())
                                .toStatus(h.getToStatus())
                                .reasonCode(h.getReasonCode())
                                .changedByType(h.getChangedByType())
                                .createdAt(h.getCreatedAt())
                                .build())
                        .toList();
        return OrderStatusResponseDTO.builder()
                .orderId(orderId).status(order.getStatus()).history(history).build();
    }

    @Transactional(readOnly = true)
    public PaymentDetailsResponseDTO getPaymentDetails(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("RESOURCE_NOT_FOUND", "Order not found: " + orderId));
        boolean payable = order.getStatus() == OrderStatus.PENDING_PAYMENT
                || order.getStatus() == OrderStatus.PAYMENT_FAILED;
        return PaymentDetailsResponseDTO.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .currency(order.getCurrency())
                .totalAmount(order.getTotalAmount())
                .payable(payable)
                .build();
    }

    // ---------- Helpers ----------

    @Transactional(readOnly = true)
    protected OrderResponseDTO getByIdInternal(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("RESOURCE_NOT_FOUND", "Order not found: " + orderId));
        return OrderMapper.toOrderDto(order,
                orderItemRepository.findByOrderId(orderId),
                orderAddressRepository.findByOrderId(orderId));
    }

    private List<ResolvedLine> resolveLines(OrderCreateRequestDTO request) {
        Map<UUID, Integer> merged = new LinkedHashMap<>();
        for (OrderCreateRequestDTO.Line l : request.getLines()) {
            if (merged.putIfAbsent(l.getProductId(), l.getQuantity()) != null) {
                throw new BadRequestException("DUPLICATE_PRODUCT_LINE",
                        "Duplicate productId in order request: " + l.getProductId());
            }
        }
        List<ResolvedLine> resolved = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : merged.entrySet()) {
            ProductSnapshotDTO p = productClient.getSnapshot(e.getKey());
            if (!"ACTIVE".equalsIgnoreCase(p.getStatus())) {
                throw new ConflictException("PRODUCT_NOT_SELLABLE",
                        "Product is not sellable: " + e.getKey());
            }
            if (!pricing.getCurrency().equalsIgnoreCase(p.getCurrency())) {
                throw new BadRequestException("CURRENCY_MISMATCH",
                        "Product currency " + p.getCurrency() + " does not match order currency "
                                + pricing.getCurrency());
            }
            BigDecimal unit = p.getBasePrice().setScale(4, RoundingMode.HALF_UP);
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(e.getValue()))
                    .setScale(4, RoundingMode.HALF_UP);
            resolved.add(new ResolvedLine(e.getKey(), p.getSku(), p.getName(), unit, e.getValue(), lineTotal));
        }
        return resolved;
    }
}
