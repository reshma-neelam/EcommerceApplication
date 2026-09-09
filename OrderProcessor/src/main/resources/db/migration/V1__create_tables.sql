-- Order Processor Service schema (MySQL 8.4) - authoritative baseline (V1).
-- Conventions: CHAR(36) UUID ids, DATETIME(6) UTC timestamps, DECIMAL(19,4) money +
-- CHAR(3) currency, JSON event payloads, VARCHAR status enums, BIGINT optimistic-lock version.
-- Frozen after first shared apply; later changes use V2, V3, ...

CREATE TABLE orders (
    id CHAR(36) NOT NULL,
    order_number VARCHAR(32) NOT NULL,
    user_id CHAR(36) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING_PAYMENT',
    currency CHAR(3) NOT NULL,
    subtotal_amount DECIMAL(19,4) NOT NULL,
    discount_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    shipping_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    total_amount DECIMAL(19,4) NOT NULL,
    latest_payment_id CHAR(36) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    cancelled_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_orders_order_number UNIQUE (order_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at);
CREATE INDEX idx_orders_status_created ON orders(status, created_at);
CREATE INDEX idx_orders_latest_payment ON orders(latest_payment_id);

CREATE TABLE order_item (
    id CHAR(36) NOT NULL,
    order_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    sku_snapshot VARCHAR(64) NOT NULL,
    name_snapshot VARCHAR(255) NOT NULL,
    unit_price DECIMAL(19,4) NOT NULL,
    quantity INT NOT NULL,
    line_total DECIMAL(19,4) NOT NULL,
    currency CHAR(3) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT chk_order_item_qty CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_order_item_order ON order_item(order_id);
CREATE INDEX idx_order_item_product ON order_item(product_id);

CREATE TABLE order_address (
    id CHAR(36) NOT NULL,
    order_id CHAR(36) NOT NULL,
    address_type VARCHAR(24) NOT NULL,
    recipient_name VARCHAR(200) NOT NULL,
    line1 VARCHAR(255) NOT NULL,
    line2 VARCHAR(255) NULL,
    city VARCHAR(128) NOT NULL,
    state_region VARCHAR(128) NULL,
    postal_code VARCHAR(32) NOT NULL,
    country_code CHAR(2) NOT NULL,
    phone VARCHAR(32) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_address_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT uq_order_address_type UNIQUE (order_id, address_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_status_history (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id CHAR(36) NOT NULL,
    from_status VARCHAR(40) NULL,
    to_status VARCHAR(40) NOT NULL,
    reason_code VARCHAR(64) NULL,
    reason_text VARCHAR(500) NULL,
    changed_by_type VARCHAR(24) NOT NULL DEFAULT 'SYSTEM',
    changed_by_id VARCHAR(128) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_osh_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_osh_order_created ON order_status_history(order_id, created_at);
CREATE INDEX idx_osh_to_status_created ON order_status_history(to_status, created_at);

CREATE TABLE order_idempotency (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    order_id CHAR(36) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_order_idem_user_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT fk_order_idem_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_order_idem_expires ON order_idempotency(expires_at);

CREATE TABLE inbox_event (
    event_id CHAR(36) NOT NULL,
    source_service VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload_json JSON NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'RECEIVED',
    received_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at DATETIME(6) NULL,
    last_error VARCHAR(1000) NULL,
    PRIMARY KEY (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_inbox_status_received ON inbox_event(status, received_at);

CREATE TABLE outbox_event (
    id CHAR(36) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL DEFAULT 'ORDER',
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
CREATE INDEX idx_outbox_aggregate ON outbox_event(aggregate_id);
