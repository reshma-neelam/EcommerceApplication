package com.scaler.productcatalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.productcatalog.dto.ProductRequestDto;
import com.scaler.productcatalog.dto.ProductUpdateDto;
import com.scaler.productcatalog.enums.ProductState;
import com.scaler.productcatalog.exception.ResourceNotFoundException;
import com.scaler.productcatalog.model.Category;
import com.scaler.productcatalog.model.Product;
import com.scaler.productcatalog.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setName("Electronics");
        category.setState(ProductState.ACTIVE);

        sampleProduct = new Product();
        sampleProduct.setId(1L);
        sampleProduct.setName("Laptop");
        sampleProduct.setDescription("A fast laptop");
        sampleProduct.setPrice(new BigDecimal("999.99"));
        sampleProduct.setImageUrl("http://img/laptop.png");
        sampleProduct.setStockQuantity(10);
        sampleProduct.setCategory(category);
        sampleProduct.setState(ProductState.ACTIVE);
    }

    // ---------- GET /api/v1/products (search/list) ----------

    @Test
    void getAllProducts_returnsPagedResults() throws Exception {
        Page<Product> page = new PageImpl<>(List.of(sampleProduct), PageRequest.of(0, 20), 1);
        when(productService.searchProducts(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/products")
                        .param("query", "Laptop")
                        .param("category", "Electronics")
                        .param("state", "ACTIVE")
                        .param("minPrice", "100")
                        .param("maxPrice", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Laptop"))
                .andExpect(jsonPath("$.content[0].categoryName").value("Electronics"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(productService).searchProducts(eq("Laptop"), eq("Electronics"), eq(ProductState.ACTIVE),
                eq(new BigDecimal("100")), eq(new BigDecimal("2000")), any(Pageable.class));
    }

    @Test
    void getAllProducts_withoutFilters_returnsResults() throws Exception {
        Page<Product> page = new PageImpl<>(List.of(sampleProduct));
        when(productService.searchProducts(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Laptop"));
    }

    // ---------- GET /api/v1/products/{id} ----------

    @Test
    void getProductById_whenExists_returnsProduct() throws Exception {
        when(productService.getProductById(1L)).thenReturn(sampleProduct);

        mockMvc.perform(get("/api/v1/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(999.99))
                .andExpect(jsonPath("$.stockQuantity").value(10))
                .andExpect(jsonPath("$.categoryName").value("Electronics"))
                .andExpect(jsonPath("$.state").value("ACTIVE"));
    }

    @Test
    void getProductById_whenMissing_returns404() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/v1/products/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }

    // ---------- POST /api/v1/products ----------

    @Test
    void createProduct_withValidBody_returns201WithLocation() throws Exception {
        ProductRequestDto request = new ProductRequestDto();
        request.setName("Laptop");
        request.setDescription("A fast laptop");
        request.setPrice(new BigDecimal("999.99"));
        request.setImageUrl("http://img/laptop.png");
        request.setStockQuantity(10);
        request.setCategoryName("Electronics");

        when(productService.createProduct(any(ProductRequestDto.class))).thenReturn(sampleProduct);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/products/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"));

        verify(productService).createProduct(any(ProductRequestDto.class));
    }

    @Test
    void createProduct_withInvalidBody_returns400() throws Exception {
        ProductRequestDto request = new ProductRequestDto();
        // name missing (blank), price null, stockQuantity null, categoryName missing
        request.setDescription("no required fields");

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists())
                .andExpect(jsonPath("$.fieldErrors.stockQuantity").exists())
                .andExpect(jsonPath("$.fieldErrors.categoryName").exists());
    }

    @Test
    void createProduct_withNegativePrice_returns400() throws Exception {
        ProductRequestDto request = new ProductRequestDto();
        request.setName("Laptop");
        request.setPrice(new BigDecimal("-5"));
        request.setStockQuantity(1);
        request.setCategoryName("Electronics");

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    // ---------- PATCH /api/v1/products/{id} ----------

    @Test
    void updateProduct_whenExists_returnsUpdatedProduct() throws Exception {
        ProductUpdateDto update = new ProductUpdateDto();
        update.setName("Laptop Pro");
        update.setPrice(new BigDecimal("1299.99"));

        Product updated = new Product();
        updated.setId(1L);
        updated.setName("Laptop Pro");
        updated.setPrice(new BigDecimal("1299.99"));
        updated.setStockQuantity(10);
        updated.setCategory(sampleProduct.getCategory());
        updated.setState(ProductState.ACTIVE);

        when(productService.updateProduct(eq(1L), any(ProductUpdateDto.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop Pro"))
                .andExpect(jsonPath("$.price").value(1299.99));

        verify(productService).updateProduct(eq(1L), any(ProductUpdateDto.class));
    }

    @Test
    void updateProduct_whenMissing_returns404() throws Exception {
        ProductUpdateDto update = new ProductUpdateDto();
        update.setName("Laptop Pro");

        when(productService.updateProduct(eq(99L), any(ProductUpdateDto.class)))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(patch("/api/v1/products/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }

    @Test
    void updateProduct_withInvalidPrice_returns400() throws Exception {
        ProductUpdateDto update = new ProductUpdateDto();
        update.setPrice(new BigDecimal("0"));

        mockMvc.perform(patch("/api/v1/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    // ---------- DELETE /api/v1/products/{id} ----------

    @Test
    void deleteProduct_whenExists_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(productService, times(1)).deleteProduct(1L);
    }

    @Test
    void deleteProduct_whenMissing_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Product not found with id: 99"))
                .when(productService).deleteProduct(99L);

        mockMvc.perform(delete("/api/v1/products/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }
}
