package com.scaler.paymentprocessor.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.scaler.paymentprocessor.exception.DependencyUnavailableException;
import com.scaler.paymentprocessor.exception.NotFoundException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestClient;

class OrderPaymentDetailsClientWireMockTest {

    private WireMockServer server;
    private OrderPaymentDetailsClient orderClient;

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
        orderClient = new OrderPaymentDetailsClient(client);
    }

    @AfterEach
    void teardown() {
        server.stop();
    }

    @Test
    void fetch_success_mapsFields() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        server.stubFor(get(urlPathMatching("/internal/v1/orders/.*/payment-details"))
                .willReturn(okJson("""
                        {"orderId":"%s","userId":"%s","currency":"INR",
                         "totalAmount":120.0000,"payable":true}
                        """.formatted(orderId, userId))));

        OrderPaymentDetailsDTO details = orderClient.fetch(orderId);

        assertThat(details.getUserId()).isEqualTo(userId);
        assertThat(details.getCurrency()).isEqualTo("INR");
        assertThat(details.isPayable()).isTrue();
        assertThat(details.getTotalAmount()).isEqualByComparingTo("120.00");
    }

    @Test
    void fetch_notFound_throwsOrderNotFound() {
        server.stubFor(get(urlPathMatching("/internal/v1/orders/.*/payment-details"))
                .willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> orderClient.fetch(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", "ORDER_NOT_FOUND");
    }

    @Test
    void fetch_timeout_throwsDependencyUnavailable() {
        server.stubFor(get(urlPathMatching("/internal/v1/orders/.*/payment-details"))
                .willReturn(okJson("{}").withFixedDelay(900)));

        assertThatThrownBy(() -> orderClient.fetch(UUID.randomUUID()))
                .isInstanceOf(DependencyUnavailableException.class)
                .hasFieldOrPropertyWithValue("code", "DEPENDENCY_UNAVAILABLE");
    }
}
