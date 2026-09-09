package com.scaler.orderprocessor.dto.order;

import com.scaler.orderprocessor.enums.ChangedByType;
import com.scaler.orderprocessor.enums.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusResponseDTO {
    private UUID orderId;
    private OrderStatus status;
    private List<HistoryEntry> history;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HistoryEntry {
        private OrderStatus fromStatus;
        private OrderStatus toStatus;
        private String reasonCode;
        private ChangedByType changedByType;
        private Instant createdAt;
    }
}
