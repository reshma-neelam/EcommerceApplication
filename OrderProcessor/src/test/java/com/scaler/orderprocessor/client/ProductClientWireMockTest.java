package com.scaler.orderprocessor.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.scaler.orderprocessor.exception.ConflictException;
import com.scaler.orderprocessor.exception.DependencyUnavailableException;
import com.scaler.orderprocessor.exception.NotFoundException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestClient;

class ProductClientWireMockTest {

    private WireMockServer server;
    private ProductClient productClient;

    @BeforeEach
    void setup() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofMillis(500))
                .withReadTimeout(Duration.ofMillis(400));
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + server.port())
                .requestFactory(ClientHttpRequestFactoryBuilder.simple().build(settings))
                .build();
        productClient = new ProductClient(client);
    }

    @AfterEach
    void teardown() {
        server.stop();
    }

    @Test
    void getSnapshot_success_mapsFields() {
        UUID id = UUID.randomUUID();
        server.stubFor(get(urlPathMatching("/api/v1/products/.*")).willReturn(okJson("""
                {"id":"%s","sku":"SKU-1","name":"Widget","status":"ACTIVE",
                 "basePrice":19.9900,"currency":"INR","availableQuantity":7}
                """.formatted(id))));

        ProductSnapshotDTO snapshot = productClient.getSnapshot(id);

        assertThat(snapshot.getSku()).isEqualTo("SKU-1");
        assertThat(snapshot.getStatus()).isEqualTo("ACTIVE");
        assertThat(snapshot.getBasePrice()).isEqualByComparingTo("19.99");
        assertThat(snapshot.getCurrency()).isEqualTo("INR");
    }

    @Test
    void getSnapshot_notFound_throwsProductNotFound() {
        server.stubFor(get(urlPathMatching("/api/v1/products/.*"))
                .willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> productClient.getSnapshot(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", "PRODUCT_NOT_FOUND");
    }

    @Test
    void reserve_conflict_throwsOutOfStock() {
        server.stubFor(post(urlEqualTo("/internal/v1/inventory/reservations"))
                .willReturn(aResponse().withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"OUT_OF_STOCK\"}")));

        assertThatThrownBy(() -> productClient.reserve(UUID.randomUUID(),
                List.of(ReserveRequest.Line.builder().productId(UUID.randomUUID()).quantity(1).build())))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code", "OUT_OF_STOCK");
    }

    @Test
    void getSnapshot_timeout_throwsDependencyUnavailable() {
        server.stubFor(get(urlPathMatching("/api/v1/products/.*"))
                .willReturn(okJson("{}").withFixedDelay(900)));

        assertThatThrownBy(() -> productClient.getSnapshot(UUID.randomUUID()))
                .isInstanceOf(DependencyUnavailableException.class)
                .hasFieldOrPropertyWithValue("code", "DEPENDENCY_UNAVAILABLE");
    }

    @Test
    void releaseQuietly_swallowsErrors() {
        server.stubFor(delete(urlPathMatching("/internal/v1/inventory/reservations/.*"))
                .willReturn(aResponse().withStatus(500)));

        assertThatCode(() -> productClient.releaseQuietly(UUID.randomUUID())).doesNotThrowAnyException();
    }
}
