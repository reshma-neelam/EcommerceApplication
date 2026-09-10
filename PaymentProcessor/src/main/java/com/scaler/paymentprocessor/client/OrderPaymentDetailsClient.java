package com.scaler.paymentprocessor.client;

import com.scaler.paymentprocessor.exception.DependencyUnavailableException;
import com.scaler.paymentprocessor.exception.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OrderPaymentDetailsClient {

    private final RestClient client;

    public OrderPaymentDetailsClient(RestClient orderRestClient) {
        this.client = orderRestClient;
    }

    public OrderPaymentDetailsDTO fetch(UUID orderId) {
        try {
            OrderPaymentDetailsDTO details = client.get()
                    .uri("/internal/v1/orders/{orderId}/payment-details", orderId)
                    .retrieve()
                    .body(OrderPaymentDetailsDTO.class);
            if (details == null) {
                throw new NotFoundException("ORDER_NOT_FOUND", "Order not found: " + orderId);
            }
            return details;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new NotFoundException("ORDER_NOT_FOUND", "Order not found: " + orderId);
            }
            throw new DependencyUnavailableException("DEPENDENCY_UNAVAILABLE",
                    "Order Processor returned an unexpected error");
        } catch (ResourceAccessException ex) {
            throw new DependencyUnavailableException("DEPENDENCY_UNAVAILABLE",
                    "Order Processor is unavailable");
        }
    }
}
