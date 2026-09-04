package com.scaler.productcatalog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.productcatalog.dto.product.ProductResponseDTO;
import com.scaler.productcatalog.enums.ProductStatus;
import com.scaler.productcatalog.exception.NotFoundException;
import com.scaler.productcatalog.service.ProductService;
import com.scaler.productcatalog.observability.CorrelationIdFilter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    private ProductResponseDTO sample(UUID id) {
        return ProductResponseDTO.builder()
                .id(id)
                .sku("SKU-1")
                .name("Laptop")
                .description("A fast laptop")
                .brand("Acme")
                .status(ProductStatus.ACTIVE)
                .basePrice(new BigDecimal("999.99"))
                .currency("USD")
                .categoryIds(List.of(UUID.randomUUID()))
                .primaryCategoryId(UUID.randomUUID())
                .images(List.of())
                .availableQuantity(10)
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void getById_whenExists_returnsProductAndCorrelationHeader() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getActive(id)).thenReturn(sample(id));

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.HEADER))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.sku").value("SKU-1"))
                .andExpect(jsonPath("$.availableQuantity").value(10));
    }

    @Test
    void getById_whenMissing_returns404WithApiError() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getActive(id)).thenThrow(new NotFoundException("PRODUCT_NOT_FOUND", "Product not found: " + id));

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void getById_withMalformedUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void create_withValidBody_returns201WithLocation() throws Exception {
        UUID id = UUID.randomUUID();
        UUID category = UUID.randomUUID();
        when(productService.create(any())).thenReturn(sample(id));

        Map<String, Object> body = Map.of(
                "sku", "SKU-1",
                "name", "Laptop",
                "basePrice", 999.99,
                "currency", "USD",
                "categoryIds", List.of(category.toString()),
                "primaryCategoryId", category.toString());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/products/" + id)));
    }

    @Test
    void create_withMissingFields_returns400WithFieldErrors() throws Exception {
        Map<String, Object> body = Map.of("description", "no required fields");

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.sku").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.basePrice").exists())
                .andExpect(jsonPath("$.fieldErrors.currency").exists())
                .andExpect(jsonPath("$.fieldErrors.primaryCategoryId").exists());
    }

    @Test
    void create_withNegativePrice_returns400() throws Exception {
        UUID category = UUID.randomUUID();
        Map<String, Object> body = Map.of(
                "sku", "SKU-2",
                "name", "Laptop",
                "basePrice", -5,
                "currency", "USD",
                "categoryIds", List.of(category.toString()),
                "primaryCategoryId", category.toString());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.basePrice").exists());
    }

    @Test
    void delete_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/products/{id}", id))
                .andExpect(status().isNoContent());
        verify(productService).softDelete(id);
    }

    @Test
    void delete_whenMissing_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new NotFoundException("PRODUCT_NOT_FOUND", "Product not found: " + id))
                .when(productService).softDelete(id);

        mockMvc.perform(delete("/api/v1/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void addImage_withNonHttpUrl_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, Object> body = Map.of("url", "ftp://example.com/x.png");

        mockMvc.perform(post("/api/v1/products/{id}/images", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.url").exists());
    }

    @Test
    void update_withoutExpectedVersion_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, Object> body = Map.of("name", "New name");

        mockMvc.perform(patch("/api/v1/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.expectedVersion").exists());
    }
}
