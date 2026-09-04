package com.scaler.productcatalog.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.scaler.productcatalog.dto.category.CategoryCreateRequestDTO;
import com.scaler.productcatalog.dto.inventory.ReservationRequestDTO;
import com.scaler.productcatalog.dto.product.ProductCreateRequestDTO;
import com.scaler.productcatalog.exception.ConflictException;
import com.scaler.productcatalog.model.ProductInventory;
import com.scaler.productcatalog.repository.ProductInventoryRepository;
import com.scaler.productcatalog.service.CategoryService;
import com.scaler.productcatalog.service.InventoryService;
import com.scaler.productcatalog.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InventoryConcurrencyTest {

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

    @Test
    void concurrentReservations_cannotOversell() throws Exception {
        TestDataCleaner.clean(jdbcTemplate);
        UUID categoryId = categoryService.create(
                new CategoryCreateRequestDTO("Cat", "cat-" + UUID.randomUUID(), null)).getId();
        UUID productId = productService.create(new ProductCreateRequestDTO(
                "SKU-CONC", "P", null, null, new BigDecimal("10.00"), "USD",
                List.of(categoryId), categoryId, 10)).getId();

        int threads = 2;
        int quantityEach = 6; // 6 + 6 > 10, so only one can succeed
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger outOfStock = new AtomicInteger();

        Callable<Void> task = () -> {
            start.await();
            try {
                inventoryService.reserve(new ReservationRequestDTO(UUID.randomUUID(), null,
                        List.of(new ReservationRequestDTO.Line(productId, quantityEach))));
                successes.incrementAndGet();
            } catch (ConflictException ex) {
                if ("OUT_OF_STOCK".equals(ex.getCode())) {
                    outOfStock.incrementAndGet();
                }
            }
            return null;
        };

        Future<Void> f1 = pool.submit(task);
        Future<Void> f2 = pool.submit(task);
        start.countDown();
        f1.get();
        f2.get();
        pool.shutdown();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(outOfStock.get()).isEqualTo(1);
        int reserved = inventoryRepository.findById(productId)
                .map(ProductInventory::getReservedQuantity).orElseThrow();
        assertThat(reserved).isEqualTo(quantityEach);
    }
}
