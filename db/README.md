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
- Timestamps use `DATETIME(6)` in UTC.
- Money uses `DECIMAL(19,4)` plus a `CHAR(3)` ISO-4217 currency code.
- Event payloads use native `JSON`.
- Boolean flags use `BOOLEAN` (`TINYINT(1)` in MySQL).
- The Product Catalog `V1` baseline is frozen; later changes use `V2`, `V3`, ...

## Product Catalog migration

The active Flyway migration at `ProductCatalog/src/main/resources/db/migration/V1__create_tables.sql`
is the executable source of truth. The reference copy in `migration/product_catalog/` is kept
byte-identical to it. The service reads env vars `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME`
/ `DB_PASSWORD` (see `.env.example`) and runs `classpath:db/migration` on startup under the
`local` and `docker` profiles.

## Quick validation

Load a reference script into a scratch MySQL database:

```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS schema_check;"
Get-Content db/migration/product_catalog/V1__create_tables.sql | mysql -u root -p schema_check
mysql -u root -p schema_check -e "SHOW TABLES;"
```
