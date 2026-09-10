CREATE TABLE payment (
    id                    CHAR(36)      NOT NULL,
    order_id              CHAR(36)      NOT NULL,
    user_id               CHAR(36)      NOT NULL,
    status                VARCHAR(40)   NOT NULL,
    currency              CHAR(3)       NOT NULL,
    amount                DECIMAL(19,4) NOT NULL,
    idempotency_key       VARCHAR(80)   NOT NULL,
    provider_payment_intent_id VARCHAR(120) NULL,
    provider_status       VARCHAR(60)   NULL,
    failure_reason        VARCHAR(255)  NULL,
    version               BIGINT        NOT NULL DEFAULT 0,
    created_at            DATETIME(6)   NOT NULL,
    updated_at            DATETIME(6)   NOT NULL,
    CONSTRAINT pk_payment PRIMARY KEY (id),
    CONSTRAINT uq_payment_order UNIQUE (order_id),
    CONSTRAINT uq_payment_user_idem UNIQUE (user_id, idempotency_key)
);
CREATE INDEX ix_payment_status ON payment (status, created_at);
CREATE INDEX ix_payment_intent ON payment (provider_payment_intent_id);

CREATE TABLE webhook_event (
    id                 CHAR(36)     NOT NULL,
    provider_event_id  VARCHAR(120) NOT NULL,
    event_type         VARCHAR(80)  NOT NULL,
    status             VARCHAR(40)  NOT NULL,
    payment_id         CHAR(36)     NULL,
    received_at        DATETIME(6)  NOT NULL,
    processed_at       DATETIME(6)  NULL,
    CONSTRAINT pk_webhook_event PRIMARY KEY (id),
    CONSTRAINT uq_webhook_provider_event UNIQUE (provider_event_id)
);
CREATE INDEX ix_webhook_status ON webhook_event (status, received_at);

CREATE TABLE outbox_event (
    id             CHAR(36)     NOT NULL,
    aggregate_id   CHAR(36)     NOT NULL,
    aggregate_type VARCHAR(40)  NOT NULL,
    event_type     VARCHAR(80)  NOT NULL,
    payload        JSON         NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    retry_count    INT          NOT NULL DEFAULT 0,
    last_error     VARCHAR(500) NULL,
    created_at     DATETIME(6)  NOT NULL,
    published_at   DATETIME(6)  NULL,
    CONSTRAINT pk_pay_outbox_event PRIMARY KEY (id)
);
CREATE INDEX ix_pay_outbox_status ON outbox_event (status, created_at);
CREATE INDEX ix_pay_outbox_aggregate ON outbox_event (aggregate_id);
