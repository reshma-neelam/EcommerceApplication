CREATE TABLE IF NOT EXISTS product (
    id CHAR(36) NOT NULL PRIMARY KEY,
    sku VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    brand VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    base_price DECIMAL(19,4) NOT NULL,
    currency CHAR(3) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_product_sku UNIQUE (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_product_status_deleted ON product(status, deleted_at);
CREATE INDEX idx_product_name ON product(name);

CREATE TABLE IF NOT EXISTS category (
    id CHAR(36) NOT NULL PRIMARY KEY,
    parent_category_id CHAR(36),
    name VARCHAR(128) NOT NULL,
    slug VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_category_slug UNIQUE (slug),
    FOREIGN KEY (parent_category_id) REFERENCES category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_category_parent ON category(parent_category_id);
CREATE INDEX idx_category_status ON category(status);

CREATE TABLE IF NOT EXISTS product_category (
    product_id CHAR(36) NOT NULL,
    category_id CHAR(36) NOT NULL,
    is_primary TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id, category_id),
    FOREIGN KEY (product_id) REFERENCES product(id),
    FOREIGN KEY (category_id) REFERENCES category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_prodcat_category ON product_category(category_id, product_id);

CREATE TABLE IF NOT EXISTS product_inventory (
    product_id CHAR(36) NOT NULL PRIMARY KEY,
    on_hand_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    reorder_level INT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (product_id) REFERENCES product(id),
    CHECK (on_hand_quantity >= 0),
    CHECK (reserved_quantity >= 0),
    CHECK (reserved_quantity <= on_hand_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS inventory_reservation (
    id CHAR(36) NOT NULL PRIMARY KEY,
    order_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE UNIQUE INDEX uq_reservation_order_product ON inventory_reservation(order_id, product_id);
CREATE INDEX idx_reservation_status_expires ON inventory_reservation(status, expires_at);
CREATE INDEX idx_reservation_product_status ON inventory_reservation(product_id, status);

CREATE TABLE IF NOT EXISTS product_image (
    id CHAR(36) NOT NULL PRIMARY KEY,
    product_id CHAR(36) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    alt_text VARCHAR(255),
    display_order INT NOT NULL DEFAULT 0,
    is_primary TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_image_product_order ON product_image(product_id, display_order);

CREATE TABLE IF NOT EXISTS outbox_event (
    id CHAR(36) NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id CHAR(36) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'NEW',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at DATETIME,
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_outbox_status_created ON outbox_event(status, created_at);
CREATE INDEX idx_outbox_aggregate ON outbox_event(aggregate_type, aggregate_id);
