package com.scaler.usermanagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the full application context against a real MySQL 8.4 container so the
 * Flyway migrations apply from an empty database and Hibernate
 * {@code ddl-auto=validate} succeeds against the migrated schema. Runs under
 * {@code mvn verify} (failsafe) and requires a running Docker engine.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class UserManagementMysqlMigrationIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("user_management_db");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("security.jwt.secret", () -> "it-secret-that-is-at-least-32-bytes-long-000000");
        registry.add("security.admin-bootstrap.enabled", () -> "false");
    }

    @Test
    void contextLoadsWithFlywayMigratedSchema() {
        // Success = Flyway migrated empty MySQL DB + Hibernate validated all UserManagement entities.
    }
}
