package com.scaler.paymentprocessor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the full application context against a real MySQL 8.4 container so that the
 * Flyway migrations apply from an empty database and Hibernate {@code ddl-auto=validate}
 * succeeds against the migrated schema. Runs under {@code mvn verify} (failsafe) and
 * requires a running Docker engine.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PaymentProcessorMysqlMigrationIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("payment_processor_db");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("payment.stripe.secret-key", () -> "sk_test_dummy");
        registry.add("payment.stripe.webhook-secret", () -> "whsec_test_dummy");
    }

    @Test
    void contextLoadsWithFlywayMigratedSchema() {
        // Success means Flyway migrated the empty MySQL database and Hibernate
        // validated the entity mappings against it during context startup.
    }
}
