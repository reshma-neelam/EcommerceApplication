package com.scaler.productcatalog.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scaler.productcatalog.dto.category.CategoryCreateRequestDTO;
import com.scaler.productcatalog.dto.category.CategoryResponseDTO;
import com.scaler.productcatalog.dto.product.ProductCreateRequestDTO;
import com.scaler.productcatalog.dto.product.ProductResponseDTO;
import com.scaler.productcatalog.dto.product.ProductUpdateRequestDTO;
import com.scaler.productcatalog.exception.BadRequestException;
import com.scaler.productcatalog.exception.ConflictException;
import com.scaler.productcatalog.exception.NotFoundException;
import com.scaler.productcatalog.repository.OutboxEventRepository;
import com.scaler.productcatalog.repository.ProductCategoryRepository;
import com.scaler.productcatalog.repository.ProductInventoryRepository;
import com.scaler.productcatalog.service.CategoryService;
import com.scaler.productcatalog.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProductCatalogIntegrationTest {

    @Autowired
    private ProductService productService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ProductInventoryRepository inventoryRepository;
    @Autowired
    private ProductCategoryRepository productCategoryRepository;
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID categoryId;

    @BeforeEach
    void setUp() {
        TestDataCleaner.clean(jdbcTemplate);
        CategoryResponseDTO category = categoryService.create(
                new CategoryCreateRequestDTO("Electronics", "electronics-" + UUID.randomUUID(), null));
        categoryId = category.getId();
    }

    private ProductCreateRequestDTO newProduct(String sku, int stock) {
        return new ProductCreateRequestDTO(sku, "Laptop", "Fast laptop", "Acme",
                new BigDecimal("999.99"), "usd", List.of(categoryId), categoryId, stock);
    }

    @Test
    void create_persistsProductCategoryInventoryAndOutbox() {
        ProductResponseDTO created = productService.create(newProduct("SKU-CREATE", 25));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getCurrency()).isEqualTo("USD");
        assertThat(created.getPrimaryCategoryId()).isEqualTo(categoryId);
        assertThat(created.getAvailableQuantity()).isEqualTo(25);
        assertThat(inventoryRepository.findById(created.getId())).isPresent();
        assertThat(productCategoryRepository.findByIdProductId(created.getId())).hasSize(1);
        assertThat(outboxEventRepository.findAll())
                .anyMatch(e -> e.getEventType().equals("ProductCreated.v1")
                        && e.getPayload().get("sku").equals("SKU-CREATE"));
    }

    @Test
    void create_withDuplicateSku_throwsConflict() {
        productService.create(newProduct("SKU-DUP", 5));
        assertThatThrownBy(() -> productService.create(newProduct("SKU-DUP", 5)))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code", "DUPLICATE_SKU");
    }

    @Test
    void create_withPrimaryNotInCategoryIds_throwsBadRequest() {
        ProductCreateRequestDTO bad = new ProductCreateRequestDTO("SKU-BADPRIMARY", "P", null, null,
                new BigDecimal("10.00"), "USD", List.of(categoryId), UUID.randomUUID(), 0);
        assertThatThrownBy(() -> productService.create(bad))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_PRIMARY_CATEGORY");
    }

    @Test
    void update_withStaleVersion_throwsConflict() {
        ProductResponseDTO created = productService.create(newProduct("SKU-VER", 5));
        productService.update(created.getId(),
                new ProductUpdateRequestDTO("Renamed", null, null, null, null, null, null, null, created.getVersion()));

        assertThatThrownBy(() -> productService.update(created.getId(),
                new ProductUpdateRequestDTO("Again", null, null, null, null, null, null, null, created.getVersion())))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code", "OPTIMISTIC_LOCK");
    }

    @Test
    void searchActive_hidesSoftDeletedProducts() {
        ProductResponseDTO keep = productService.create(newProduct("SKU-KEEP", 5));
        ProductResponseDTO remove = productService.create(newProduct("SKU-REMOVE", 5));
        productService.softDelete(remove.getId());

        var page = productService.searchActive(null, categoryId, null, null,
                PageRequest.of(0, 20, Sort.by("createdAt").descending()));

        assertThat(page.getContent()).extracting(ProductResponseDTO::getId).contains(keep.getId());
        assertThat(page.getContent()).extracting(ProductResponseDTO::getId).doesNotContain(remove.getId());
        assertThatThrownBy(() -> productService.getActive(remove.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void searchActive_withInvalidPriceRange_throwsBadRequest() {
        assertThatThrownBy(() -> productService.searchActive(null, null,
                new BigDecimal("100"), new BigDecimal("10"), PageRequest.of(0, 20)))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_PRICE_RANGE");
    }

    @Test
    void searchActive_withTooLargePageSize_throwsBadRequest() {
        assertThatThrownBy(() -> productService.searchActive(null, null, null, null,
                PageRequest.of(0, 500)))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_PAGE_SIZE");
    }
}
