CREATE TABLE IF NOT EXISTS notification_template (
    id CHAR(36) NOT NULL PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    channel VARCHAR(24) NOT NULL DEFAULT 'EMAIL',
    locale VARCHAR(16) NOT NULL DEFAULT 'en-US',
    subject_template VARCHAR(500),
    body_template TEXT NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    template_version INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE UNIQUE INDEX uq_template_event_channel ON notification_template(event_type, channel, locale, template_version);
CREATE INDEX idx_template_active ON notification_template(active, event_type, channel, locale);

CREATE TABLE IF NOT EXISTS consumed_event (
    event_id CHAR(36) NOT NULL PRIMARY KEY,
    source_service VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'RECEIVED',
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME,
    last_error VARCHAR(1000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_consumed_status_received ON consumed_event(status, received_at);

CREATE TABLE IF NOT EXISTS notification_job (
    id CHAR(36) NOT NULL PRIMARY KEY,
    event_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    order_id CHAR(36),
    event_type VARCHAR(128) NOT NULL,
    channel VARCHAR(24) NOT NULL DEFAULT 'EMAIL',
    recipient VARCHAR(500) NOT NULL,
    template_id CHAR(36),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    payload_json TEXT NOT NULL,
    scheduled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME,
    FOREIGN KEY (template_id) REFERENCES notification_template(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE UNIQUE INDEX uq_job_event_channel ON notification_job(event_id, channel);
CREATE INDEX idx_job_status_scheduled ON notification_job(status, scheduled_at);
CREATE INDEX idx_job_user_created ON notification_job(user_id, created_at);

CREATE TABLE IF NOT EXISTS notification_delivery_attempt (
    id CHAR(36) NOT NULL PRIMARY KEY,
    notification_job_id CHAR(36) NOT NULL,
    attempt_no INT NOT NULL,
    provider VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL,
    provider_message_id VARCHAR(255),
    error_code VARCHAR(128),
    error_message VARCHAR(1000),
    attempted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (notification_job_id) REFERENCES notification_job(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE UNIQUE INDEX uq_delivery_job_attempt ON notification_delivery_attempt(notification_job_id, attempt_no);
CREATE INDEX idx_delivery_provider_msg ON notification_delivery_attempt(provider_message_id);
