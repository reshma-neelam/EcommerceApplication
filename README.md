# Ecommerce Application

Java / Spring Boot microservices monorepo.

## Project Structure

```
EcommerceApplication/
  ProductCatalog/          # Product Catalog Service
  db/migration/            # Reference SQL schemas for all 5 services
  docs/                    # Design documents (LLD, DB Schema)
```

## Prerequisites

- **Java 21** (JDK)
- **MySQL 8+** server running locally (or accessible remotely)

## Quick Start

### 1. Create the database

Connect to your MySQL server and run:

```sql
CREATE DATABASE IF NOT EXISTS product_catalog_db;
```

### 2. Run the service

```powershell
cd ProductCatalog
.\mvnw.cmd spring-boot:run
```

This uses the defaults: `localhost:3306`, user `root`, password `password`.

### 3. Override DB credentials (if your MySQL setup differs)

Edit `ProductCatalog/src/main/resources/application-mysql.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/product_catalog_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_password
```

Alternatively, you can use environment variables (works on all OS):

| Variable      | Default              | Description    |
|---------------|----------------------|----------------|
| `DB_HOST`     | `localhost`          | MySQL host     |
| `DB_PORT`     | `3306`               | MySQL port     |
| `DB_NAME`     | `product_catalog_db` | Database name  |
| `DB_USERNAME` | `root`               | MySQL username |
| `DB_PASSWORD` | `password`           | MySQL password |

### 4. Verify

```
GET  http://localhost:8080/api/v1/products          # List all products
POST http://localhost:8080/api/v1/products           # Create a product
GET  http://localhost:8080/api/v1/products/{id}      # Get product by ID
```

**Sample POST body:**

```json
{
  "name": "iPhone 15",
  "description": "Apple smartphone",
  "price": 79999.00,
  "imageUrl": "https://example.com/iphone15.jpg",
  "stockQuantity": 50,
  "categoryName": "Electronics"
}
```

## Configuration

Tables are auto-created on first startup via Flyway migrations + Hibernate.

## Build

```powershell
cd ProductCatalog
.\mvnw.cmd clean package -DskipTests
```
