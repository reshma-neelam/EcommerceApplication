package com.scaler.paymentprocessor.gateway;

public interface PaymentGateway {
    CreatePaymentResult createPayment(CreatePaymentCommand command);
}
