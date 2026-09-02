# Database migrations (MySQL)

This folder holds SQL schema references for the ecommerce services. All services target **MySQL 8+** only (InnoDB, `utf8mb4`).

## Layout

- `migration/product_catalog/` - Product Catalog schema reference
- `migration/user_management/` - User Management schema reference
- `migration/order_processor/` - Order Processor schema reference
- `migration/payment_processor/` - Payment Processor schema reference
- `migration/notification/` - Notification schema reference
- `../ProductCatalog/src/main/resources/db/migration/` - active Flyway migration for the ProductCatalog service (kept in sync with its JPA entities)

## Conventions

- Storage engine `InnoDB` (required for foreign keys and transactional integrity).
- Character set `utf8mb4`.
- UUID primary and foreign keys stored as `CHAR(36)`.
- Timestamps use `DATETIME`.
- JSON-like payloads use `TEXT`.
- Boolean flags use `TINYINT(1)` (0 = false, 1 = true).
- Scripts use `CREATE TABLE IF NOT EXISTS` for repeatable local setup.

## ProductCatalog profile

The ProductCatalog service runs on the `mysql` profile only. Connection settings live in `ProductCatalog/src/main/resources/application-mysql.properties` and read env vars `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` (defaults `localhost` / `3306` / `product_catalog_db` / `root` / `root`). Flyway runs `classpath:db/migration` on startup.

## Quick validation

Load a reference script into a scratch MySQL database:

```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS schema_check;"
Get-Content db/migration/product_catalog/V1__create_tables.sql | mysql -u root -p schema_check
mysql -u root -p schema_check -e "SHOW TABLES;"
```
