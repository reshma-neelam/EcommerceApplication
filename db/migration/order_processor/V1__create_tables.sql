CREATE TABLE IF NOT EXISTS orders (
    id CHAR(36) NOT NULL PRIMARY KEY,
    order_number VARCHAR(32) NOT NULL,
    user_id CHAR(36) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING_PAYMENT',
    currency CHAR(3) NOT NULL,
    subtotal_amount DECIMAL(19,4) NOT NULL,
    discount_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    shipping_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    total_amount DECIMAL(19,4) NOT NULL,
    latest_payment_id CHAR(36),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at DATETIME,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_orders_number UNIQUE (order_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at);
CREATE INDEX idx_orders_status_created ON orders(status, created_at);

CREATE TABLE IF NOT EXISTS order_item (
    id CHAR(36) NOT NULL PRIMARY KEY,
    order_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    sku_snapshot VARCHAR(64) NOT NULL,
    name_snapshot VARCHAR(255) NOT NULL,
    unit_price DECIMAL(19,4) NOT NULL,
    quantity INT NOT NULL,
    line_total DECIMAL(19,4) NOT NULL,
    currency CHAR(3) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_orderitem_order ON order_item(order_id);
CREATE INDEX idx_orderitem_product ON order_item(product_id);

CREATE TABLE IF NOT EXISTS order_address (
    id CHAR(36) NOT NULL PRIMARY KEY,
    order_id CHAR(36) NOT NULL,
    address_type VARCHAR(24) NOT NULL,
    recipient_name VARCHAR(200) NOT NULL,
    line1 VARCHAR(255) NOT NULL,
    line2 VARCHAR(255),
    city VARCHAR(128) NOT NULL,
    state_region VARCHAR(128),
    postal_code VARCHAR(32) NOT NULL,
    country_code CHAR(2) NOT NULL,
    phone VARCHAR(32),
    FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE UNIQUE INDEX uq_orderaddr_order_type ON order_address(order_id, address_type);

CREATE TABLE IF NOT EXISTS order_status_history (
    id CHAR(36) NOT NULL PRIMARY KEY,
    order_id CHAR(36) NOT NULL,
    from_status VARCHAR(40),
    to_status VARCHAR(40) NOT NULL,
    reason_code VARCHAR(64),
    reason_text VARCHAR(500),
    changed_by_type VARCHAR(24) NOT NULL DEFAULT 'SYSTEM',
    changed_by_id VARCHAR(128),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_statushist_order_created ON order_status_history(order_id, created_at);
CREATE INDEX idx_statushist_tostatus ON order_status_history(to_status, created_at);

CREATE TABLE IF NOT EXISTS order_idempotency (
    id CHAR(36) NOT NULL PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    order_id CHAR(36),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE UNIQUE INDEX uq_idempotency_user_key ON order_idempotency(user_id, idempotency_key);
CREATE INDEX idx_idempotency_expires ON order_idempotency(expires_at);

CREATE TABLE IF NOT EXISTS inbox_event (
    event_id CHAR(36) NOT NULL PRIMARY KEY,
    source_service VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'RECEIVED',
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME,
    last_error VARCHAR(1000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_inbox_status_received ON inbox_event(status, received_at);

CREATE TABLE IF NOT EXISTS outbox_event (
    id CHAR(36) NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL DEFAULT 'ORDER',
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
