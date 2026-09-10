package com.scaler.paymentprocessor.gateway;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatePaymentResult {
    private String providerPaymentIntentId;
    private String clientSecret;
    private String providerStatus;
}
