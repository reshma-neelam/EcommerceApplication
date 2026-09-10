package com.scaler.paymentprocessor.dto.payment;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePaymentRequestDTO {
    @NotNull
    private UUID orderId;
}
