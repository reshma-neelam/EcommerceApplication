package com.scaler.paymentprocessor.dto;

import java.time.Instant;
import java.util.Map;
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
public class ApiErrorDTO {
    private String code;
    private String message;
    private String correlationId;
    private Instant timestamp;
    private Map<String, String> fieldErrors;
}
