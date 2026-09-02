CREATE TABLE IF NOT EXISTS payment (
    id CHAR(36) NOT NULL PRIMARY KEY,
    order_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    provider VARCHAR(32) NOT NULL DEFAULT 'STRIPE',
    provider_payment_intent_id VARCHAR(255),
    status VARCHAR(40) NOT NULL DEFAULT 'INITIATED',
    amount DECIMAL(19,4) NOT NULL,
    currency CHAR(3) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    version BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE UNIQUE INDEX uq_payment_provider_intent ON payment(provider, provider_payment_intent_id);
CREATE UNIQUE INDEX uq_payment_order_idemp ON payment(order_id, idempotency_key);
CREATE INDEX idx_payment_order_status ON payment(order_id, status);

CREATE TABLE IF NOT EXISTS payment_attempt (
    id CHAR(36) NOT NULL PRIMARY KEY,
    payment_id CHAR(36) NOT NULL,
    attempt_no INT NOT NULL,
    provider_charge_id VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    failure_code VARCHAR(128),
    failure_message VARCHAR(1000),
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    FOREIGN KEY (payment_id) REFERENCES payment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE UNIQUE INDEX uq_attempt_payment_no ON payment_attempt(payment_id, attempt_no);
CREATE INDEX idx_attempt_charge ON payment_attempt(provider_charge_id);

CREATE TABLE IF NOT EXISTS payment_webhook_event (
    id CHAR(36) NOT NULL PRIMARY KEY,
    provider VARCHAR(32) NOT NULL DEFAULT 'STRIPE',
    provider_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload_json TEXT NOT NULL,
    processing_status VARCHAR(24) NOT NULL DEFAULT 'RECEIVED',
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME,
    error_message VARCHAR(1000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE UNIQUE INDEX uq_webhook_provider_event ON payment_webhook_event(provider, provider_event_id);
CREATE INDEX idx_webhook_status_received ON payment_webhook_event(processing_status, received_at);

CREATE TABLE IF NOT EXISTS payment_refund (
    id CHAR(36) NOT NULL PRIMARY KEY,
    payment_id CHAR(36) NOT NULL,
    order_id CHAR(36) NOT NULL,
    provider_refund_id VARCHAR(255),
    amount DECIMAL(19,4) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    reason VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    FOREIGN KEY (payment_id) REFERENCES payment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_refund_payment_status ON payment_refund(payment_id, status);
CREATE INDEX idx_refund_order ON payment_refund(order_id);

CREATE TABLE IF NOT EXISTS inbox_event (
    event_id CHAR(36) NOT NULL PRIMARY KEY,
    source_service VARCHAR(64) NOT NULL DEFAULT 'ORDER',
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
    aggregate_type VARCHAR(64) NOT NULL DEFAULT 'PAYMENT',
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
