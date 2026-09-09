package com.scaler.orderprocessor.client;

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
public class ReserveRequest {
    private UUID orderId;
    private Integer expiresInMinutes;
    private List<Line> lines;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Line {
        private UUID productId;
        private Integer quantity;
    }
}
