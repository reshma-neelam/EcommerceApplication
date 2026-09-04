package com.scaler.productcatalog.dto.inventory;

import com.scaler.productcatalog.enums.ReservationStatus;
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
public class ReservationResponseDTO {

    private UUID orderId;
    private List<Line> lines;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Line {
        private UUID reservationId;
        private UUID productId;
        private int quantity;
        private ReservationStatus status;
        private Instant expiresAt;
    }
}
