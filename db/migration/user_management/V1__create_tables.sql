CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) NOT NULL PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    email_verified_at DATETIME,
    last_login_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_email UNIQUE (email)
);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_created ON users(created_at);

CREATE TABLE IF NOT EXISTS user_credential (
    user_id CHAR(36) NOT NULL PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    password_algorithm VARCHAR(32) NOT NULL DEFAULT 'ARGON2ID',
    password_changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    failed_login_count INT NOT NULL DEFAULT 0,
    locked_until DATETIME,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_credential_locked ON user_credential(locked_until);

CREATE TABLE IF NOT EXISTS user_session (
    id CHAR(36) NOT NULL PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    refresh_token_hash CHAR(64) NOT NULL,
    issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME,
    client_fingerprint VARCHAR(255),
    CONSTRAINT uq_session_token UNIQUE (refresh_token_hash),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_session_user_revoked ON user_session(user_id, revoked_at);
CREATE INDEX IF NOT EXISTS idx_session_expires ON user_session(expires_at);

CREATE TABLE IF NOT EXISTS user_address (
    id CHAR(36) NOT NULL PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    address_type VARCHAR(24) NOT NULL DEFAULT 'SHIPPING',
    recipient_name VARCHAR(200) NOT NULL,
    line1 VARCHAR(255) NOT NULL,
    line2 VARCHAR(255),
    city VARCHAR(128) NOT NULL,
    state_region VARCHAR(128),
    postal_code VARCHAR(32) NOT NULL,
    country_code CHAR(2) NOT NULL,
    phone VARCHAR(32),
    is_default INTEGER NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_address_user_type ON user_address(user_id, address_type);
CREATE INDEX IF NOT EXISTS idx_address_user_default ON user_address(user_id, is_default);

CREATE TABLE IF NOT EXISTS role (
    id CHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT uq_role_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS user_role (
    user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES role(id)
);

CREATE TABLE IF NOT EXISTS outbox_event (
    id CHAR(36) NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL DEFAULT 'USER',
    aggregate_id CHAR(36) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'NEW',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at DATETIME,
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000)
);
CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON outbox_event(status, created_at);
