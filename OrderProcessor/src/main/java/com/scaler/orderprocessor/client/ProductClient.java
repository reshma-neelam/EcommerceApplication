package com.scaler.orderprocessor.client;

import com.scaler.orderprocessor.exception.ApiException;
import com.scaler.orderprocessor.exception.ConflictException;
import com.scaler.orderprocessor.exception.DependencyUnavailableException;
import com.scaler.orderprocessor.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ProductClient {

    private final RestClient client;

    public ProductClient(RestClient productCatalogRestClient) {
        this.client = productCatalogRestClient;
    }

    public ProductSnapshotDTO getSnapshot(UUID productId) {
        try {
            ProductSnapshotDTO snapshot = client.get()
                    .uri("/api/v1/products/{id}", productId)
                    .retrieve()
                    .body(ProductSnapshotDTO.class);
            if (snapshot == null) {
                throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not found: " + productId);
            }
            return snapshot;
        } catch (RestClientResponseException ex) {
            throw mapStatus(ex, "PRODUCT_NOT_FOUND", "Product not found: " + productId);
        } catch (ResourceAccessException ex) {
            throw unavailable();
        }
    }

    public void reserve(UUID orderId, List<ReserveRequest.Line> lines) {
        ReserveRequest request = ReserveRequest.builder().orderId(orderId).lines(lines).build();
        try {
            client.post()
                    .uri("/internal/v1/inventory/reservations")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw new ConflictException("OUT_OF_STOCK",
                        "Inventory could not be reserved for order " + orderId);
            }
            throw mapStatus(ex, "PRODUCT_NOT_FOUND", "One or more products were not found");
        } catch (ResourceAccessException ex) {
            throw unavailable();
        }
    }

    /** Compensation: idempotent release; never throws to the caller's failure path. */
    public void releaseQuietly(UUID orderId) {
        try {
            client.delete()
                    .uri("/internal/v1/inventory/reservations/{orderId}", orderId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ignored) {
            // best-effort compensation; a later reservation-expiry worker (Week 7) reclaims stock
        }
    }

    private ApiException mapStatus(RestClientResponseException ex, String notFoundCode, String notFoundMsg) {
        if (ex.getStatusCode().value() == 404) {
            return new NotFoundException(notFoundCode, notFoundMsg);
        }
        return new DependencyUnavailableException("DEPENDENCY_UNAVAILABLE",
                "Product Catalog returned an unexpected error");
    }

    private DependencyUnavailableException unavailable() {
        return new DependencyUnavailableException("DEPENDENCY_UNAVAILABLE",
                "Product Catalog is unavailable");
    }
}
