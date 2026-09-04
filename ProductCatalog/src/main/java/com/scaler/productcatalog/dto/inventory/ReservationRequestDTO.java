package com.scaler.productcatalog.dto.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class ReservationRequestDTO {

    @NotNull(message = "orderId is required")
    private UUID orderId;

    @Min(value = 1, message = "expiresInMinutes must be >= 1")
    private Integer expiresInMinutes;

    @NotEmpty(message = "at least one line is required")
    @Valid
    private List<Line> lines;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Line {

        @NotNull(message = "productId is required")
        private UUID productId;

        @NotNull
        @Min(value = 1, message = "quantity must be >= 1")
        private Integer quantity;
    }
}
