-- Product Catalog Service schema (MySQL 8.4) - authoritative baseline (V1).
-- Conventions (DB Schema Design section 2): CHAR(36) UUID aggregate ids,
-- DATETIME(6) UTC timestamps, DECIMAL(19,4) money + CHAR(3) currency,
-- JSON event payloads, VARCHAR status enums, BIGINT optimistic-lock version.
-- Frozen after first shared apply; later changes use V2, V3, ...

CREATE TABLE product (
    id CHAR(36) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    brand VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    base_price DECIMAL(19,4) NOT NULL,
    currency CHAR(3) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_product_sku UNIQUE (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_product_status_deleted ON product(status, deleted_at);
CREATE INDEX idx_product_name ON product(name);

CREATE TABLE category (
    id CHAR(36) NOT NULL,
    parent_category_id CHAR(36) NULL,
    name VARCHAR(128) NOT NULL,
    slug VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_category_slug UNIQUE (slug),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_category_id) REFERENCES category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_category_parent ON category(parent_category_id);
CREATE INDEX idx_category_status ON category(status);

CREATE TABLE product_category (
    product_id CHAR(36) NOT NULL,
    category_id CHAR(36) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (product_id, category_id),
    CONSTRAINT fk_pc_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_pc_category FOREIGN KEY (category_id) REFERENCES category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_pc_category_product ON product_category(category_id, product_id);

CREATE TABLE product_inventory (
    product_id CHAR(36) NOT NULL,
    on_hand_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    reorder_level INT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (product_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT chk_on_hand_nonneg CHECK (on_hand_quantity >= 0),
    CONSTRAINT chk_reserved_nonneg CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_reserved_le_on_hand CHECK (reserved_quantity <= on_hand_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inventory_reservation (
    id CHAR(36) NOT NULL,
    order_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_reservation_order_product UNIQUE (order_id, product_id),
    CONSTRAINT fk_reservation_product FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_reservation_status_expires ON inventory_reservation(status, expires_at);
CREATE INDEX idx_reservation_product_status ON inventory_reservation(product_id, status);

CREATE TABLE product_image (
    id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    alt_text VARCHAR(255) NULL,
    display_order INT NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_image_product FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_image_product_order ON product_image(product_id, display_order);

CREATE TABLE outbox_event (
    id CHAR(36) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id CHAR(36) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload_json JSON NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'NEW',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at DATETIME(6) NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_outbox_status_created ON outbox_event(status, created_at);
CREATE INDEX idx_outbox_aggregate ON outbox_event(aggregate_type, aggregate_id);
CREATE INDEX idx_outbox_event_type ON outbox_event(event_type);
