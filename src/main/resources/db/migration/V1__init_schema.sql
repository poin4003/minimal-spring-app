-- Initial schema for the minimal Spring Boot project.
-- Scope: API + H2 + JobRunr-ready application tables only.

CREATE DOMAIN UserStatusEnum AS VARCHAR(8)
    CHECK (VALUE IN ('ACTIVE', 'INACTIVE', 'LOCKED'));

CREATE DOMAIN SimStatusEnum AS VARCHAR(8)
    CHECK (VALUE IN ('ACTIVE', 'INACTIVE', 'BLOCKED', 'PICKED', 'DELETED'));

CREATE DOMAIN CronjobStatusEnum AS VARCHAR(8)
    CHECK (VALUE IN ('ACTIVE', 'INACTIVE'));

CREATE TABLE permission (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    permission_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_permission_permission_key ON permission(permission_key);

CREATE TABLE role (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    role_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_role_role_key ON role(role_key);

CREATE TABLE user_base (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    status UserStatusEnum NOT NULL,
    login_time TIMESTAMP,
    logout_time TIMESTAMP,
    login_ip VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_user_base_email ON user_base(email);

CREATE TABLE user_info (
    id UUID PRIMARY KEY,
    username VARCHAR(255),
    phone_number VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_info_user
        FOREIGN KEY (id) REFERENCES user_base(id)
        ON DELETE CASCADE
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES role(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permission(id)
        ON DELETE CASCADE
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES user_base(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES role(id)
        ON DELETE CASCADE
);

CREATE TABLE key_store (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    signing_key VARCHAR(255) NOT NULL,
    refresh_token VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_key_store_user
        FOREIGN KEY (user_id) REFERENCES user_base(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_key_store_user_id ON key_store(user_id);
CREATE UNIQUE INDEX uk_key_store_refresh_token ON key_store(refresh_token);

CREATE TABLE consumed_refresh_token (
    id UUID PRIMARY KEY,
    key_store_id UUID NOT NULL,
    user_id UUID NOT NULL,
    token_value VARCHAR(512),
    expiry_date TIMESTAMP WITH TIME ZONE,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_consumed_refresh_token_key_store
        FOREIGN KEY (key_store_id) REFERENCES key_store(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_consumed_refresh_token_user
        FOREIGN KEY (user_id) REFERENCES user_base(id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_consumed_refresh_token_token_value
    ON consumed_refresh_token(token_value);

CREATE INDEX idx_consumed_refresh_token_expiry_date
    ON consumed_refresh_token(expiry_date);

CREATE TABLE sim (
    id UUID PRIMARY KEY,
    phone_number VARCHAR(255) NOT NULL,
    status SimStatusEnum NOT NULL,
    selling_price INTEGER,
    dealer_price INTEGER,
    import_price INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_sim_phone_number ON sim(phone_number);

CREATE TABLE cronjob_config (
    id UUID PRIMARY KEY,
    job_type VARCHAR(255) NOT NULL,
    expression VARCHAR(100),
    status CronjobStatusEnum NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_cronjob_config_job_type ON cronjob_config(job_type);
