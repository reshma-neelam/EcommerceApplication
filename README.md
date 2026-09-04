# Ecommerce Application

Java / Spring Boot microservices monorepo, built as a multi-module Maven project
with a single root Maven wrapper.

## Planning and Deployment

- [Low-level design](docs/Ecommerce_LLD_Design.md)
- [Database schema design](docs/Ecommerce_DB_Schema_Design.md)
- [Week-by-week implementation plan](docs/Ecommerce_Implementation_Plan.md)
- [Living implementation tracker](docs/Ecommerce_Implementation_Tracker.md)
- [Current implementation changes](docs/Ecommerce_Current_Implementation_Changes.md)
- [Docker and AWS learning deployment guide](docs/Ecommerce_Docker_AWS_Deployment_Guide.md)

## Project Structure

```
EcommerceApplication/
  pom.xml                  # Root parent POM (centralized versions, modules)
  mvnw / mvnw.cmd / .mvn/  # Single root Maven wrapper
  compose.deps.yaml        # Dependency-only stack (MySQL 8.4 + Kafka KRaft)
  .env.example             # Copy to .env for local overrides
  ProductCatalog/          # Product Catalog Service
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

## Verify a running service

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
