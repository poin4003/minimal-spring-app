-- Initial schema for the minimal Spring Boot project.
-- Scope: API + H2 + JobRunr-ready application tables only.

CREATE DOMAIN UserStatusEnum AS VARCHAR(8)
    CHECK (VALUE IN ('ACTIVE', 'INACTIVE', 'LOCKED'));

CREATE DOMAIN SimStatusEnum AS VARCHAR(8)
    CHECK (VALUE IN ('ACTIVE', 'INACTIVE', 'BLOCKED', 'PICKED', 'DELETED'));

CREATE DOMAIN RecordStatus AS VARCHAR(8)
    CHECK (VALUE IN ('ACTIVE', 'INACTIVE'));

CREATE DOMAIN MediaKindEnum AS VARCHAR(16)
    CHECK (VALUE IN ('IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT', 'FILE'));

CREATE DOMAIN MediaProcessingStatusEnum AS VARCHAR(16)
    CHECK (VALUE IN ('PENDING', 'READY', 'FAILED'));

CREATE DOMAIN MediaVariantTypeEnum AS VARCHAR(16)
    CHECK (VALUE IN ('HLS_PLAYLIST'));

CREATE TABLE permission (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    permission_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_permission_permission_key ON permission(permission_key);
CREATE INDEX idx_permission_created_at ON permission(created_at);
CREATE INDEX idx_permission_updated_at ON permission(updated_at);

CREATE TABLE role (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    role_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_role_role_key ON role(role_key);
CREATE INDEX idx_role_created_at ON role(created_at);
CREATE INDEX idx_role_updated_at ON role(updated_at);

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
CREATE INDEX idx_user_base_created_at ON user_base(created_at);
CREATE INDEX idx_user_base_updated_at ON user_base(updated_at);

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

CREATE INDEX idx_user_info_created_at ON user_info(created_at);
CREATE INDEX idx_user_info_updated_at ON user_info(updated_at);

CREATE TABLE media (
    id UUID PRIMARY KEY,
    created_by UUID NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    public_key VARCHAR(64) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    kind MediaKindEnum NOT NULL,
    processing_status MediaProcessingStatusEnum NOT NULL,
    status RecordStatus NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_media_created_by
        FOREIGN KEY (created_by) REFERENCES user_base(id)
);

CREATE UNIQUE INDEX uk_media_storage_key ON media(storage_key);
CREATE UNIQUE INDEX uk_media_public_key ON media(public_key);
CREATE INDEX idx_media_created_by_created_at ON media(created_by, created_at);
CREATE INDEX idx_media_status_created_at ON media(status, created_at);
CREATE INDEX idx_media_processing_status_created_at ON media(processing_status, created_at);
CREATE INDEX idx_media_created_at ON media(created_at);
CREATE INDEX idx_media_updated_at ON media(updated_at);

CREATE TABLE media_variant (
    id UUID PRIMARY KEY,
    media_id UUID NOT NULL,
    variant_type MediaVariantTypeEnum NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    width INTEGER,
    height INTEGER,
    bitrate INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_media_variant_media
        FOREIGN KEY (media_id) REFERENCES media(id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_media_variant_storage_key ON media_variant(storage_key);
CREATE UNIQUE INDEX uk_media_variant_media_type ON media_variant(media_id, variant_type);
CREATE INDEX idx_media_variant_created_at ON media_variant(created_at);
CREATE INDEX idx_media_variant_updated_at ON media_variant(updated_at);

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
CREATE INDEX idx_key_store_created_at ON key_store(created_at);
CREATE INDEX idx_key_store_updated_at ON key_store(updated_at);

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

CREATE INDEX idx_consumed_refresh_token_created_at
    ON consumed_refresh_token(created_at);

CREATE INDEX idx_consumed_refresh_token_updated_at
    ON consumed_refresh_token(updated_at);

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
CREATE INDEX idx_sim_created_at ON sim(created_at);
CREATE INDEX idx_sim_updated_at ON sim(updated_at);

CREATE TABLE cronjob_config (
    id UUID PRIMARY KEY,
    job_type VARCHAR(255) NOT NULL,
    expression VARCHAR(100),
    status RecordStatus NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_cronjob_config_job_type ON cronjob_config(job_type);
CREATE INDEX idx_cronjob_config_created_at ON cronjob_config(created_at);
CREATE INDEX idx_cronjob_config_updated_at ON cronjob_config(updated_at);
