package com.scaler.productcatalog.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scaler.productcatalog.dto.category.CategoryCreateRequestDTO;
import com.scaler.productcatalog.dto.inventory.ReservationRequestDTO;
import com.scaler.productcatalog.dto.inventory.ReservationResponseDTO;
import com.scaler.productcatalog.dto.product.ProductCreateRequestDTO;
import com.scaler.productcatalog.exception.ConflictException;
import com.scaler.productcatalog.model.ProductInventory;
import com.scaler.productcatalog.repository.ProductInventoryRepository;
import com.scaler.productcatalog.service.CategoryService;
import com.scaler.productcatalog.service.InventoryService;
import com.scaler.productcatalog.service.ProductService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InventoryReservationTest {

    @Autowired
    private ProductService productService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private ProductInventoryRepository inventoryRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID categoryId;

    @BeforeEach
    void setUp() {
        TestDataCleaner.clean(jdbcTemplate);
        categoryId = categoryService.create(
                new CategoryCreateRequestDTO("Cat", "cat-" + UUID.randomUUID(), null)).getId();
    }

    private UUID createProduct(String sku, int stock) {
        return productService.create(new ProductCreateRequestDTO(sku, "P", null, null,
                new BigDecimal("10.00"), "USD", List.of(categoryId), categoryId, stock)).getId();
    }

    private int reserved(UUID productId) {
        return inventoryRepository.findById(productId).map(ProductInventory::getReservedQuantity).orElseThrow();
    }

    @Test
    void reserve_thenReplaySameBody_isIdempotent() {
        UUID productId = createProduct("SKU-IDEMP", 10);
        UUID orderId = UUID.randomUUID();
        ReservationRequestDTO request = new ReservationRequestDTO(orderId, null,
                List.of(new ReservationRequestDTO.Line(productId, 3)));

        ReservationResponseDTO first = inventoryService.reserve(request);
        ReservationResponseDTO replay = inventoryService.reserve(request);

        assertThat(replay.getLines()).hasSize(1);
        assertThat(replay.getLines().get(0).getReservationId())
                .isEqualTo(first.getLines().get(0).getReservationId());
        assertThat(reserved(productId)).isEqualTo(3);
    }

    @Test
    void reserve_sameOrderDifferentQuantity_returns409() {
        UUID productId = createProduct("SKU-DIFF", 10);
        UUID orderId = UUID.randomUUID();
        inventoryService.reserve(new ReservationRequestDTO(orderId, null,
                List.of(new ReservationRequestDTO.Line(productId, 3))));

        assertThatThrownBy(() -> inventoryService.reserve(new ReservationRequestDTO(orderId, null,
                List.of(new ReservationRequestDTO.Line(productId, 4)))))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code", "DUPLICATE_REQUEST");
    }

    @Test
    void reserve_multiLine_isAllOrNothing() {
        UUID a = createProduct("SKU-A", 10);
        UUID b = createProduct("SKU-B", 1);
        UUID orderId = UUID.randomUUID();

        assertThatThrownBy(() -> inventoryService.reserve(new ReservationRequestDTO(orderId, null, List.of(
                new ReservationRequestDTO.Line(a, 5),
                new ReservationRequestDTO.Line(b, 5)))))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code", "OUT_OF_STOCK");

        // Neither line was committed.
        assertThat(reserved(a)).isZero();
        assertThat(reserved(b)).isZero();
    }

    @Test
    void release_isIdempotentAndReturnsStock() {
        UUID productId = createProduct("SKU-REL", 10);
        UUID orderId = UUID.randomUUID();
        inventoryService.reserve(new ReservationRequestDTO(orderId, null,
                List.of(new ReservationRequestDTO.Line(productId, 4))));
        assertThat(reserved(productId)).isEqualTo(4);

        inventoryService.release(orderId);
        inventoryService.release(orderId); // replay must not double-decrement

        assertThat(reserved(productId)).isZero();
    }

    @Test
    void expireDue_releasesStockOnceAndMarksExpired() {
        UUID productId = createProduct("SKU-EXP", 10);
        UUID orderId = UUID.randomUUID();
        inventoryService.reserve(new ReservationRequestDTO(orderId, 30,
                List.of(new ReservationRequestDTO.Line(productId, 6))));
        assertThat(reserved(productId)).isEqualTo(6);

        // Cutoff far in the future forces the active reservation to be treated as due.
        Instant future = Instant.now().plus(60, ChronoUnit.MINUTES);
        int expired = inventoryService.expireDue(future);
        int expiredAgain = inventoryService.expireDue(future);

        assertThat(expired).isEqualTo(1);
        assertThat(expiredAgain).isZero();
        assertThat(reserved(productId)).isZero();
    }

    @Test
    void reserve_isRejectedWhenInsufficientStock() {
        UUID productId = createProduct("SKU-LOW", 2);
        UUID orderId = UUID.randomUUID();
        assertThatThrownBy(() -> inventoryService.reserve(new ReservationRequestDTO(orderId, null,
                List.of(new ReservationRequestDTO.Line(productId, 5)))))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code", "OUT_OF_STOCK");
        assertThat(reserved(productId)).isZero();
    }
}
