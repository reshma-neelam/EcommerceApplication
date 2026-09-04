package com.scaler.productcatalog.integration;

import org.springframework.jdbc.core.JdbcTemplate;

/** Clears Product Catalog tables between integration tests for isolation. */
final class TestDataCleaner {

    private TestDataCleaner() {
    }

    static void clean(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("DELETE FROM inventory_reservation");
        jdbcTemplate.execute("DELETE FROM product_category");
        jdbcTemplate.execute("DELETE FROM product_image");
        jdbcTemplate.execute("DELETE FROM product_inventory");
        jdbcTemplate.execute("DELETE FROM outbox_event");
        jdbcTemplate.execute("DELETE FROM product");
        jdbcTemplate.execute("DELETE FROM category");
    }
}
