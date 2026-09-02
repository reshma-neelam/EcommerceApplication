-- Schema for ProductCatalog service (MySQL). Column types match the JPA entities
-- Product and Category (both extend BaseModel) so Hibernate ddl-auto=validate passes.

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT,
    state VARCHAR(32),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    name VARCHAR(255),
    description VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX idx_categories_name ON categories(name);

CREATE TABLE IF NOT EXISTS products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT,
    state VARCHAR(32),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    name VARCHAR(255),
    description VARCHAR(255),
    price DECIMAL(19,2),
    image_url VARCHAR(1024),
    stock_quantity INTEGER NOT NULL,
    category_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB;

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_state ON products(state);
CREATE INDEX idx_products_name ON products(name);
