-- Catalog posts, flexible attributes, offers, and social post references.

CREATE DOMAIN CatalogAttributeValueTypeEnum AS VARCHAR(16)
    CONSTRAINT ck_catalog_attribute_value_type_value
    CHECK (VALUE IN (
        'TEXT',
        'NUMBER',
        'MONEY',
        'BOOLEAN',
        'OPTION',
        'URL',
        'PHONE',
        'DATE'
    ));

CREATE DOMAIN CatalogReferenceTypeEnum AS VARCHAR(16)
    CONSTRAINT ck_catalog_reference_type_value
    CHECK (VALUE IN ('REVIEW', 'MENTION', 'PROMOTION'));

CREATE TABLE catalog_attribute (
    id UUID PRIMARY KEY,
    attribute_key VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    value_type CatalogAttributeValueTypeEnum NOT NULL,
    unit VARCHAR(32),
    searchable BOOLEAN DEFAULT TRUE NOT NULL,
    status RecordStatus DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_catalog_attribute_key_not_blank CHECK (
        CHAR_LENGTH(TRIM(attribute_key)) > 0
    ),
    CONSTRAINT ck_catalog_attribute_name_not_blank CHECK (
        CHAR_LENGTH(TRIM(name)) > 0
    )
);

CREATE UNIQUE INDEX uk_catalog_attribute_key
    ON catalog_attribute(attribute_key);

CREATE INDEX idx_catalog_attribute_status_name
    ON catalog_attribute(status, name);

CREATE TABLE catalog_category (
    id UUID PRIMARY KEY,
    parent_id UUID,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(160) NOT NULL,
    description TEXT,
    sort_order INTEGER DEFAULT 0 NOT NULL,
    status RecordStatus DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_catalog_category_parent
        FOREIGN KEY (parent_id) REFERENCES catalog_category(id)
        ON DELETE SET NULL,
    CONSTRAINT ck_catalog_category_name_not_blank CHECK (
        CHAR_LENGTH(TRIM(name)) > 0
    ),
    CONSTRAINT ck_catalog_category_slug_not_blank CHECK (
        CHAR_LENGTH(TRIM(slug)) > 0
    ),
    CONSTRAINT ck_catalog_category_sort_order CHECK (sort_order >= 0)
);

CREATE UNIQUE INDEX uk_catalog_category_slug
    ON catalog_category(slug);

CREATE INDEX idx_catalog_category_status_order
    ON catalog_category(status, sort_order);

CREATE INDEX idx_catalog_category_parent_status_order
    ON catalog_category(parent_id, status, sort_order);

CREATE TABLE catalog_attribute_option (
    id UUID PRIMARY KEY,
    attribute_id UUID NOT NULL,
    option_code VARCHAR(100) NOT NULL,
    label VARCHAR(150) NOT NULL,
    sort_order INTEGER DEFAULT 0 NOT NULL,
    status RecordStatus DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_catalog_attribute_option_attribute
        FOREIGN KEY (attribute_id) REFERENCES catalog_attribute(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_catalog_attribute_option_code_not_blank CHECK (
        CHAR_LENGTH(TRIM(option_code)) > 0
    ),
    CONSTRAINT ck_catalog_attribute_option_label_not_blank CHECK (
        CHAR_LENGTH(TRIM(label)) > 0
    ),
    CONSTRAINT ck_catalog_attribute_option_sort_order CHECK (sort_order >= 0)
);

CREATE UNIQUE INDEX uk_catalog_attribute_option_code
    ON catalog_attribute_option(attribute_id, option_code);

CREATE INDEX idx_catalog_attr_option_status_order
    ON catalog_attribute_option(attribute_id, status, sort_order);

CREATE TABLE catalog_category_attribute (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL,
    attribute_id UUID NOT NULL,
    required BOOLEAN DEFAULT FALSE NOT NULL,
    filterable BOOLEAN DEFAULT FALSE NOT NULL,
    multiple_values_allowed BOOLEAN DEFAULT FALSE NOT NULL,
    custom_option_allowed BOOLEAN DEFAULT FALSE NOT NULL,
    sort_order INTEGER DEFAULT 0 NOT NULL,
    CONSTRAINT fk_catalog_category_attribute_category
        FOREIGN KEY (category_id) REFERENCES catalog_category(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_catalog_category_attribute_attribute
        FOREIGN KEY (attribute_id) REFERENCES catalog_attribute(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_catalog_category_attribute_sort_order CHECK (
        sort_order >= 0
    )
);

CREATE UNIQUE INDEX uk_catalog_category_attribute
    ON catalog_category_attribute(category_id, attribute_id);

CREATE INDEX idx_catalog_cat_attr_category_filter_order
    ON catalog_category_attribute(category_id, filterable, sort_order);

CREATE INDEX idx_catalog_cat_attr_attribute_category
    ON catalog_category_attribute(attribute_id, category_id);

CREATE TABLE catalog_post (
    post_id UUID PRIMARY KEY,
    category_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    CONSTRAINT fk_catalog_post_post
        FOREIGN KEY (post_id) REFERENCES post(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_catalog_post_category
        FOREIGN KEY (category_id) REFERENCES catalog_category(id)
        ON DELETE SET NULL,
    CONSTRAINT ck_catalog_post_title_not_blank CHECK (
        CHAR_LENGTH(TRIM(title)) > 0
    )
);

CREATE INDEX idx_catalog_post_category
    ON catalog_post(category_id);

CREATE TABLE catalog_offer (
    id UUID PRIMARY KEY,
    catalog_post_id UUID NOT NULL,
    name VARCHAR(255),
    is_default BOOLEAN DEFAULT FALSE NOT NULL,
    position INTEGER DEFAULT 0 NOT NULL,
    status RecordStatus DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_catalog_offer_post
        FOREIGN KEY (catalog_post_id) REFERENCES catalog_post(post_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_catalog_offer_position CHECK (position >= 0)
);

CREATE UNIQUE INDEX uk_catalog_offer_position
    ON catalog_offer(catalog_post_id, position);

CREATE INDEX idx_catalog_offer_post_status_position
    ON catalog_offer(catalog_post_id, status, position);

CREATE INDEX idx_catalog_offer_post_default_status
    ON catalog_offer(catalog_post_id, is_default, status);

CREATE TABLE catalog_post_attribute_value (
    id UUID PRIMARY KEY,
    catalog_post_id UUID NOT NULL,
    attribute_id UUID,
    custom_key VARCHAR(100),
    custom_name VARCHAR(150),
    value_type CatalogAttributeValueTypeEnum NOT NULL,
    text_value TEXT,
    number_value NUMERIC(24, 6),
    boolean_value BOOLEAN,
    date_value DATE,
    option_id UUID,
    unit VARCHAR(32),
    position INTEGER DEFAULT 0 NOT NULL,
    CONSTRAINT fk_catalog_post_attr_value_post
        FOREIGN KEY (catalog_post_id) REFERENCES catalog_post(post_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_catalog_post_attr_value_attribute
        FOREIGN KEY (attribute_id) REFERENCES catalog_attribute(id),
    CONSTRAINT fk_catalog_post_attr_value_option
        FOREIGN KEY (option_id) REFERENCES catalog_attribute_option(id),
    CONSTRAINT ck_catalog_post_attr_value_position CHECK (position >= 0),
    CONSTRAINT ck_catalog_post_attr_value_reference CHECK (
        (
            attribute_id IS NOT NULL
            AND custom_key IS NULL
            AND custom_name IS NULL
        )
        OR (
            attribute_id IS NULL
            AND custom_key IS NOT NULL
            AND CHAR_LENGTH(TRIM(custom_key)) > 0
            AND custom_name IS NOT NULL
            AND CHAR_LENGTH(TRIM(custom_name)) > 0
        )
    ),
    CONSTRAINT ck_catalog_post_attr_value_typed CHECK (
        (
            value_type IN ('TEXT', 'URL', 'PHONE')
            AND text_value IS NOT NULL
            AND CHAR_LENGTH(TRIM(text_value)) > 0
            AND number_value IS NULL
            AND boolean_value IS NULL
            AND date_value IS NULL
            AND option_id IS NULL
        )
        OR (
            value_type IN ('NUMBER', 'MONEY')
            AND text_value IS NULL
            AND number_value IS NOT NULL
            AND boolean_value IS NULL
            AND date_value IS NULL
            AND option_id IS NULL
        )
        OR (
            value_type = 'BOOLEAN'
            AND text_value IS NULL
            AND number_value IS NULL
            AND boolean_value IS NOT NULL
            AND date_value IS NULL
            AND option_id IS NULL
        )
        OR (
            value_type = 'DATE'
            AND text_value IS NULL
            AND number_value IS NULL
            AND boolean_value IS NULL
            AND date_value IS NOT NULL
            AND option_id IS NULL
        )
        OR (
            value_type = 'OPTION'
            AND number_value IS NULL
            AND boolean_value IS NULL
            AND date_value IS NULL
            AND (
                (
                    option_id IS NOT NULL
                    AND text_value IS NULL
                )
                OR (
                    option_id IS NULL
                    AND text_value IS NOT NULL
                    AND CHAR_LENGTH(TRIM(text_value)) > 0
                )
            )
        )
    )
);

CREATE UNIQUE INDEX uk_catalog_post_attr_value
    ON catalog_post_attribute_value(
        catalog_post_id,
        attribute_id,
        position
    );

CREATE UNIQUE INDEX uk_catalog_post_custom_value
    ON catalog_post_attribute_value(
        catalog_post_id,
        custom_key,
        position
    );

CREATE INDEX idx_catalog_post_attr_value_order
    ON catalog_post_attribute_value(catalog_post_id, position);

CREATE INDEX idx_catalog_post_attr_number
    ON catalog_post_attribute_value(
        attribute_id,
        number_value,
        catalog_post_id
    );

CREATE INDEX idx_catalog_post_attr_option
    ON catalog_post_attribute_value(
        attribute_id,
        option_id,
        catalog_post_id
    );

CREATE INDEX idx_catalog_post_attr_boolean
    ON catalog_post_attribute_value(
        attribute_id,
        boolean_value,
        catalog_post_id
    );

CREATE INDEX idx_catalog_post_attr_date
    ON catalog_post_attribute_value(
        attribute_id,
        date_value,
        catalog_post_id
    );

CREATE INDEX idx_catalog_post_attr_custom_key
    ON catalog_post_attribute_value(custom_key, catalog_post_id);

CREATE TABLE catalog_offer_attribute_value (
    id UUID PRIMARY KEY,
    catalog_offer_id UUID NOT NULL,
    attribute_id UUID,
    custom_key VARCHAR(100),
    custom_name VARCHAR(150),
    value_type CatalogAttributeValueTypeEnum NOT NULL,
    text_value TEXT,
    number_value NUMERIC(24, 6),
    boolean_value BOOLEAN,
    date_value DATE,
    option_id UUID,
    unit VARCHAR(32),
    position INTEGER DEFAULT 0 NOT NULL,
    CONSTRAINT fk_catalog_offer_attr_value_offer
        FOREIGN KEY (catalog_offer_id) REFERENCES catalog_offer(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_catalog_offer_attr_value_attribute
        FOREIGN KEY (attribute_id) REFERENCES catalog_attribute(id),
    CONSTRAINT fk_catalog_offer_attr_value_option
        FOREIGN KEY (option_id) REFERENCES catalog_attribute_option(id),
    CONSTRAINT ck_catalog_offer_attr_value_position CHECK (position >= 0),
    CONSTRAINT ck_catalog_offer_attr_value_reference CHECK (
        (
            attribute_id IS NOT NULL
            AND custom_key IS NULL
            AND custom_name IS NULL
        )
        OR (
            attribute_id IS NULL
            AND custom_key IS NOT NULL
            AND CHAR_LENGTH(TRIM(custom_key)) > 0
            AND custom_name IS NOT NULL
            AND CHAR_LENGTH(TRIM(custom_name)) > 0
        )
    ),
    CONSTRAINT ck_catalog_offer_attr_value_typed CHECK (
        (
            value_type IN ('TEXT', 'URL', 'PHONE')
            AND text_value IS NOT NULL
            AND CHAR_LENGTH(TRIM(text_value)) > 0
            AND number_value IS NULL
            AND boolean_value IS NULL
            AND date_value IS NULL
            AND option_id IS NULL
        )
        OR (
            value_type IN ('NUMBER', 'MONEY')
            AND text_value IS NULL
            AND number_value IS NOT NULL
            AND boolean_value IS NULL
            AND date_value IS NULL
            AND option_id IS NULL
        )
        OR (
            value_type = 'BOOLEAN'
            AND text_value IS NULL
            AND number_value IS NULL
            AND boolean_value IS NOT NULL
            AND date_value IS NULL
            AND option_id IS NULL
        )
        OR (
            value_type = 'DATE'
            AND text_value IS NULL
            AND number_value IS NULL
            AND boolean_value IS NULL
            AND date_value IS NOT NULL
            AND option_id IS NULL
        )
        OR (
            value_type = 'OPTION'
            AND number_value IS NULL
            AND boolean_value IS NULL
            AND date_value IS NULL
            AND (
                (
                    option_id IS NOT NULL
                    AND text_value IS NULL
                )
                OR (
                    option_id IS NULL
                    AND text_value IS NOT NULL
                    AND CHAR_LENGTH(TRIM(text_value)) > 0
                )
            )
        )
    )
);

CREATE UNIQUE INDEX uk_catalog_offer_attr_value
    ON catalog_offer_attribute_value(
        catalog_offer_id,
        attribute_id,
        position
    );

CREATE UNIQUE INDEX uk_catalog_offer_custom_value
    ON catalog_offer_attribute_value(
        catalog_offer_id,
        custom_key,
        position
    );

CREATE INDEX idx_catalog_offer_attr_value_order
    ON catalog_offer_attribute_value(catalog_offer_id, position);

CREATE INDEX idx_catalog_offer_attr_number
    ON catalog_offer_attribute_value(
        attribute_id,
        number_value,
        catalog_offer_id
    );

CREATE INDEX idx_catalog_offer_attr_option
    ON catalog_offer_attribute_value(
        attribute_id,
        option_id,
        catalog_offer_id
    );

CREATE INDEX idx_catalog_offer_attr_boolean
    ON catalog_offer_attribute_value(
        attribute_id,
        boolean_value,
        catalog_offer_id
    );

CREATE INDEX idx_catalog_offer_attr_date
    ON catalog_offer_attribute_value(
        attribute_id,
        date_value,
        catalog_offer_id
    );

CREATE INDEX idx_catalog_offer_attr_custom_key
    ON catalog_offer_attribute_value(custom_key, catalog_offer_id);

CREATE TABLE post_catalog_reference (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    catalog_post_id UUID NOT NULL,
    reference_type CatalogReferenceTypeEnum NOT NULL,
    position INTEGER DEFAULT 0 NOT NULL,
    CONSTRAINT fk_post_catalog_reference_post
        FOREIGN KEY (post_id) REFERENCES post(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_post_catalog_reference_catalog_post
        FOREIGN KEY (catalog_post_id) REFERENCES catalog_post(post_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_post_catalog_reference_position CHECK (position >= 0)
);

CREATE UNIQUE INDEX uk_post_catalog_reference_target
    ON post_catalog_reference(post_id, catalog_post_id);

CREATE UNIQUE INDEX uk_post_catalog_reference_position
    ON post_catalog_reference(post_id, position);

CREATE INDEX idx_post_catalog_ref_catalog_type_post
    ON post_catalog_reference(catalog_post_id, reference_type, post_id);
