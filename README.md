# Ecommerce Application

Java / Spring Boot microservices monorepo, built as a multi-module Maven project
with a single root Maven wrapper.

## Planning and Deployment

- [Low-level design](docs/Ecommerce_LLD_Design.docx)
- [Database schema design](docs/Ecommerce_DB_Schema_Design.docx)

## Project Structure

```
EcommerceApplication/
  pom.xml                  # Root parent POM (centralized versions, modules)
  mvnw / mvnw.cmd / .mvn/  # Single root Maven wrapper
  compose.deps.yaml        # Dependency-only stack (MySQL 8.4 + Kafka KRaft)
  .env.example             # Copy to .env for local overrides
  ProductCatalog/          # Product Catalog Service (port 8080)
  UserManagement/          # User Management Service - identity, auth, JWT (port 8081)
  OrderProcessor/          # Order Processor Service - order lifecycle, snapshots (port 8082)
  bruno/                   # Bruno REST API collections (ProductCatalog, UserManagement, OrderProcessor)
  db/migration/            # Reference SQL schemas for all 5 services
  docs/                    # Design documents (LLD, DB Schema)
```

> Module layout note: the Product Catalog service intentionally keeps its
> existing capitalized folder name `ProductCatalog` rather than being renamed to
> a lowercase module layout. This avoids a disruptive path/history rewrite at
> this stage; future services follow the agreed lowercase layout.

## Prerequisites

- **Java 21** (JDK)
- **Docker** (for the dependency stack and Testcontainers integration tests)
- MySQL is provided by `compose.deps.yaml`; a separately installed MySQL is optional.

## Build and test

All commands run from the repository root using the root wrapper.

```powershell
# Green build with the self-contained test suite (H2 only, no MySQL/Docker needed)
.\mvnw.cmd test

# Full verify, including the Testcontainers MySQL migration test (requires Docker)
.\mvnw.cmd verify

# Package all modules without tests
.\mvnw.cmd clean package -DskipTests
```

`mvn test` uses the `test` profile (in-memory H2) and never contacts MySQL,
Kafka, or any external provider. `mvn verify` additionally runs a Testcontainers
integration test that applies the Flyway migrations against a real MySQL 8.4
container and validates the Hibernate mappings; it is skipped automatically when
Docker is not available.

## Local dependencies (MySQL + Kafka)

Start the backing services with Docker Compose:

```powershell
Copy-Item .env.example .env          # first time only, then edit if needed
docker compose -f compose.deps.yaml --env-file .env up -d
docker compose -f compose.deps.yaml ps          # wait for healthy status
```

Stop them:

```powershell
docker compose -f compose.deps.yaml down         # keeps the MySQL/Kafka volumes
docker compose -f compose.deps.yaml down -v      # also DELETES the data volumes
```

## Run the Product Catalog service

The application defaults to the `local` profile (MySQL on `localhost:3306`).

```powershell
.\mvnw.cmd -pl ProductCatalog spring-boot:run
```

Profiles:

| Profile | Purpose                            | MySQL host          |
|---------|------------------------------------|---------------------|
| `local` | Developer machine (default)        | `localhost`         |
| `docker`| Running inside the compose network | `mysql` (service)   |
| `test`  | Automated tests (in-memory H2)     | n/a                 |

Override the active profile with `SPRING_PROFILES_ACTIVE`, e.g. `docker`.

### Configuration via environment variables

Values come from `.env` / your shell (see `.env.example`):

| Variable      | Default              | Description    |
|---------------|----------------------|----------------|
| `DB_HOST`     | `localhost` (local)  | MySQL host     |
| `DB_PORT`     | `3306`               | MySQL port     |
| `DB_NAME`     | `product_catalog_db` | Database name  |
| `DB_USERNAME` | `root`               | MySQL username |
| `DB_PASSWORD` | `root`               | MySQL password |
| `KAFKA_PORT`  | `9092`               | Kafka port     |
| `SPRING_PROFILES_ACTIVE` | `local`   | Active Spring profile |

## Run the User Management service

Identity, authentication, and JWT authorization service. Defaults to the `local`
profile and MySQL on `localhost:3306`; it runs on port **8081** so it can run
alongside Product Catalog (8080). Its database (`user_management_db`) is created
automatically on first run (`createDatabaseIfNotExist=true`).

```powershell
.\mvnw.cmd -pl UserManagement spring-boot:run
```

The same `local` / `docker` / `test` profiles apply (see the table above). The
service exposes signup/login/refresh/logout, `GET|PATCH /api/v1/users/me`,
address CRUD under `/api/v1/users/me/addresses`, and an internal lookup at
`/internal/v1/users/{userId}`.

### Configuration via environment variables

Shares the `DB_*` variables above (with `DB_NAME` defaulting to
`user_management_db`) plus these service-specific values:

| Variable                   | Default                     | Description                                   |
|----------------------------|-----------------------------|-----------------------------------------------|
| `SERVER_PORT`              | `8081`                      | HTTP port                                     |
| `DB_NAME`                  | `user_management_db`        | Database name                                 |
| `JWT_SECRET`               | local dev placeholder       | HMAC signing secret (min 32 bytes); set in any hosted run |
| `JWT_ACCESS_TTL`           | `900`                       | Access-token TTL in seconds (15 min)          |
| `JWT_ISSUER`               | `user-management`           | JWT `iss` claim                               |
| `REFRESH_TTL_DAYS`         | `30`                        | Refresh-token lifetime in days                |
| `ADMIN_BOOTSTRAP_ENABLED`  | `false`                     | Enable the dev-only ADMIN bootstrap           |
| `ADMIN_BOOTSTRAP_EMAIL`    | _(empty)_                   | ADMIN email (required when bootstrap enabled) |
| `ADMIN_BOOTSTRAP_PASSWORD` | _(empty)_                   | ADMIN password (required when bootstrap enabled) |

### Development-only ADMIN bootstrap

By default no ADMIN user exists. For local/demo use you can idempotently create
one ADMIN account from environment variables at startup. The runner only activates when `ADMIN_BOOTSTRAP_ENABLED=true` and
both email and password are provided:

```powershell
$env:ADMIN_BOOTSTRAP_ENABLED="true"
$env:ADMIN_BOOTSTRAP_EMAIL="admin@example.com"
$env:ADMIN_BOOTSTRAP_PASSWORD="Adm1nSecret!"
.\mvnw.cmd -pl UserManagement spring-boot:run
```

Then log in with those credentials via `POST /api/v1/auth/login`; the issued JWT
carries the `ADMIN` role. Re-running is safe — an existing admin is not
duplicated. To disable it again, unset the variables (or start a new shell):

```powershell
Remove-Item Env:ADMIN_BOOTSTRAP_ENABLED, Env:ADMIN_BOOTSTRAP_EMAIL, Env:ADMIN_BOOTSTRAP_PASSWORD
```

## Run the Order Processor service

Order lifecycle service: creates orders with authoritative money and immutable
item/address snapshots, reserves inventory in Product Catalog, and validates the
first-party JWTs issued by User Management. Defaults to the `local` profile and
MySQL on `localhost:3306`; it runs on port **8082** so it can run alongside
Product Catalog (8080) and User Management (8081). Its database
(`order_processor_db`) is created automatically on first run.

Start dependencies and the upstream services first, then run the service:

```powershell
docker compose -f compose.deps.yaml up -d
# Start ProductCatalog (8080) and UserManagement (8081) first; JWT_SECRET must match UserManagement.
.\mvnw.cmd -pl OrderProcessor spring-boot:run
```

H2 demo (no MySQL, in-memory schema from JPA entities):

```powershell
.\mvnw.cmd -pl OrderProcessor spring-boot:run -Ph2demo "-Dspring-boot.run.profiles=h2"
```

Run its self-contained tests, or the MySQL Testcontainers migration test:

```powershell
.\mvnw.cmd -pl OrderProcessor test      # H2 only, no MySQL/ProductCatalog needed
.\mvnw.cmd -pl OrderProcessor verify     # adds OrderProcessorMysqlMigrationIT (needs Docker)
```

### Configuration via environment variables

Shares the `DB_*` variables above (with `DB_NAME` defaulting to
`order_processor_db`) plus these service-specific values:

| Variable                | Default                 | Description                                                    |
|-------------------------|-------------------------|----------------------------------------------------------------|
| `SERVER_PORT`           | `8082`                  | HTTP port                                                      |
| `DB_NAME`               | `order_processor_db`    | Database name                                                  |
| `JWT_SECRET`            | local dev placeholder   | HMAC secret; **must match** User Management for token validation |
| `JWT_ISSUER`            | `user-management`       | Expected JWT `iss` claim                                       |
| `PRODUCT_CATALOG_URL`   | `http://localhost:8080` | Base URL for synchronous snapshot/reservation calls           |
| `ORDER_CURRENCY`        | `INR`                   | Single configured order currency (MVP)                        |

## Verify a running service


### Product Catalog (port 8080)

```
GET    http://localhost:8080/actuator/health/liveness    # liveness probe
GET    http://localhost:8080/actuator/health/readiness   # readiness probe
GET    http://localhost:8080/api/v1/categories           # List active categories
POST   http://localhost:8080/api/v1/categories           # Create a category (ADMIN)
GET    http://localhost:8080/api/v1/products             # Search active products (paged)
POST   http://localhost:8080/api/v1/products             # Create a product (ADMIN)
GET    http://localhost:8080/api/v1/products/{productId} # Get an active product
PATCH  http://localhost:8080/api/v1/products/{productId} # Partial update (needs expectedVersion)
```

Internal inventory endpoints (Order Processor / ADMIN) live under `/internal/v1/inventory`:
adjust stock, reserve idempotently by `orderId`, and release a reservation.

Every request accepts/echoes an `X-Correlation-Id` header. Product identifiers are UUIDs.

### User Management (port 8081)

```
GET    http://localhost:8081/actuator/health/liveness      # liveness probe
GET    http://localhost:8081/actuator/health/readiness     # readiness probe
POST   http://localhost:8081/api/v1/auth/signup            # Register (assigns CUSTOMER role)
POST   http://localhost:8081/api/v1/auth/login             # Login -> access + refresh tokens
POST   http://localhost:8081/api/v1/auth/refresh           # Rotate refresh token
POST   http://localhost:8081/api/v1/auth/logout            # Revoke a refresh session
GET    http://localhost:8081/api/v1/users/me               # Current profile (Bearer JWT)
PATCH  http://localhost:8081/api/v1/users/me               # Partial profile update (Bearer JWT)
GET    http://localhost:8081/api/v1/users/me/addresses     # List addresses (Bearer JWT)
POST   http://localhost:8081/api/v1/users/me/addresses     # Add an address (Bearer JWT)
PATCH  http://localhost:8081/api/v1/users/me/addresses/{addressId}   # Update an address (Bearer JWT)
DELETE http://localhost:8081/api/v1/users/me/addresses/{addressId}   # Delete an address (Bearer JWT)
GET    http://localhost:8081/internal/v1/users/{userId}    # Internal minimal lookup (authenticated)
```

Protected endpoints require an `Authorization: Bearer <accessToken>` header;
missing/invalid/expired tokens return HTTP 401 `UNAUTHORIZED`. Ownership is
derived from the JWT subject, not the request body.

### Order Processor (port 8082)

```
GET    http://localhost:8082/actuator/health/liveness        # liveness probe
GET    http://localhost:8082/actuator/health/readiness       # readiness probe
POST   http://localhost:8082/api/v1/orders                   # Create an order (CUSTOMER, needs Idempotency-Key)
GET    http://localhost:8082/api/v1/orders                   # List own orders (CUSTOMER, paged)
GET    http://localhost:8082/api/v1/orders/{orderId}         # Get an order (owner/ADMIN)
GET    http://localhost:8082/api/v1/orders/{orderId}/status  # Order status + history (owner/ADMIN)
```

Order creation requires an `Idempotency-Key` header: the same key with the same
body replays the original order, while a different body returns HTTP 409
`DUPLICATE_REQUEST`. Money, SKU/name/price, and shipping/billing addresses are
snapshotted; a non-owning, non-ADMIN caller receives HTTP 404 `RESOURCE_NOT_FOUND`.

**Sample signup POST body:**

```json
{
  "email": "alice@example.com",
  "password": "Sup3rSecret!",
  "firstName": "Alice",
  "lastName": "Anderson"
}
```

**Sample login POST body** (response returns `accessToken`, `refreshToken`, `tokenType`, `expiresInSeconds`):

```json
{
  "email": "alice@example.com",
  "password": "Sup3rSecret!"
}
```

### Testing the APIs with Bruno

Ready-to-run [Bruno](https://www.usebruno.com/) collections live under `bruno/`:

- `bruno/ProductCatalog` — categories, products, images, search, and inventory flows.
- `bruno/UserManagement` — signup/login/refresh/logout, profile, and address flows
  (requests are chained via variables; run them top-to-bottom with the **Local** environment).
- `bruno/OrderProcessor` — login (via User Management), create/replay/conflict order,
  get/list/status, and a validation-error case (set `productId` in the **Local** environment
  to an existing Product Catalog product; run top-to-bottom).

**Sample category POST body:**

```json
{
  "name": "Electronics",
  "slug": "electronics"
}
```

**Sample product POST body** (reference existing category IDs; one must be the primary):

```json
{
  "sku": "IPHONE-15-128",
  "name": "iPhone 15",
  "description": "Apple smartphone",
  "brand": "Apple",
  "basePrice": 79999.00,
  "currency": "INR",
  "categoryIds": ["<categoryId>"],
  "primaryCategoryId": "<categoryId>",
  "initialOnHandQuantity": 50
}
```

## Configuration notes

- Schema is owned by Flyway migrations; Hibernate runs with `ddl-auto=validate`.
- SQL and debug logging are disabled by default; raise per profile or via env when debugging.
- Code formatting is defined by `.editorconfig` and the Spotless plugin
  (`.\mvnw.cmd spotless:check` / `.\mvnw.cmd spotless:apply`).
