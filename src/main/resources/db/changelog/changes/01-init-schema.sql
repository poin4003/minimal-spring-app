-- liquibase formatted sql

-- changeset pchuy:1777646662592-1
CREATE TABLE "consumed_refresh_token" (
    "created_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "expiry_date" TIMESTAMP WITH TIME ZONE,
    "updated_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "used_at" TIMESTAMP WITH TIME ZONE,
    "id" UUID NOT NULL,
    "key_store_id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "token_value" VARCHAR(255),
    CONSTRAINT "consumed_refresh_token_pkey" PRIMARY KEY ("id")
);

-- changeset pchuy:1777646662592-2
CREATE TABLE "key_store" (
    "created_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "private_key" VARCHAR(255),
    "public_key" VARCHAR(255),
    "refresh_token" VARCHAR(255),
    CONSTRAINT "key_store_pkey" PRIMARY KEY ("id")
);

-- changeset pchuy:1777646662592-3
CREATE TABLE "webhook_event" (
    "created_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "id" UUID NOT NULL,
    "event_type" VARCHAR(100) NOT NULL,
    "business_reference" VARCHAR(255),
    "payload" JSONB NOT NULL,
    CONSTRAINT "webhook_event_pkey" PRIMARY KEY ("id")
);

-- changeset pchuy:1777646662592-4
CREATE TABLE "cronjob_config" (
    "status" SMALLINT NOT NULL,
    "created_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "id" UUID NOT NULL,
    "expression" VARCHAR(255),
    "job_type" VARCHAR(255),
    "lock_at_least_for" VARCHAR(255),
    "lock_at_most_for" VARCHAR(255),
    "name" VARCHAR(255),
    CONSTRAINT "cronjob_config_pkey" PRIMARY KEY ("id")
);

-- changeset pchuy:1777646662592-5
CREATE TABLE "sim" (
    "dealer_price" INTEGER,
    "import_price" INTEGER,
    "selling_price" INTEGER,
    "status" SMALLINT NOT NULL,
    "created_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "id" UUID NOT NULL,
    "phone_number" VARCHAR(255) NOT NULL,
    CONSTRAINT "sim_pkey" PRIMARY KEY ("id")
);

-- changeset pchuy:1777646662592-6
CREATE TABLE "user_base" (
    "status" SMALLINT,
    "created_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "login_time" TIMESTAMP WITHOUT TIME ZONE,
    "logout_time" TIMESTAMP WITHOUT TIME ZONE,
    "updated_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "id" UUID NOT NULL,
    "del_flag" VARCHAR(255),
    "email" VARCHAR(255) NOT NULL,
    "login_ip" VARCHAR(255),
    "password" VARCHAR(255) NOT NULL,
    CONSTRAINT "user_base_pkey" PRIMARY KEY ("id")
);

-- changeset pchuy:1777646662592-7
CREATE TABLE "webhook_delivery_attempt" (
    "attempt_count" INTEGER NOT NULL,
    "last_http_status" INTEGER,
    "status" SMALLINT NOT NULL,
    "created_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "next_retry_at" TIMESTAMP WITHOUT TIME ZONE,
    "updated_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "subscription_id" UUID NOT NULL,
    "event_id" UUID NOT NULL,
    "id" UUID NOT NULL,
    "last_error_message" TEXT,
    CONSTRAINT "webhook_delivery_attempt_pkey" PRIMARY KEY ("id")
);

-- changeset pchuy:1777646662592-8
CREATE TABLE "webhook_subscription" (
    "base_delay_seconds" INTEGER NOT NULL,
    "max_delay_seconds" INTEGER NOT NULL,
    "max_retries" INTEGER NOT NULL,
    "max_rpm" INTEGER NOT NULL,
    "status" SMALLINT NOT NULL,
    "created_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "id" UUID NOT NULL,
    "partner_id" UUID NOT NULL,
    "partner_code" VARCHAR(50) NOT NULL,
    "url" VARCHAR(500) NOT NULL,
    "config" JSONB NOT NULL,
    CONSTRAINT "webhook_subscription_pkey" PRIMARY KEY ("id")
);

-- changeset pchuy:1777646662592-9
CREATE INDEX "idx_consumed_token_value" ON "consumed_refresh_token" ("token_value");

-- changeset pchuy:1777646662592-10
CREATE INDEX "idx_comsumed_user" ON "consumed_refresh_token" ("user_id");

-- changeset pchuy:1777646662592-11
CREATE INDEX "idx_keystore_token" ON "key_store" ("refresh_token");

-- changeset pchuy:1777646662592-12
ALTER TABLE "key_store"
ADD CONSTRAINT "idx_keystore_user" UNIQUE ("user_id");

-- changeset pchuy:1777646662592-13
CREATE INDEX "idx_webhook_event_business_ref" ON "webhook_event" ("business_reference");

-- changeset pchuy:1777646662592-14
ALTER TABLE "sim"
ADD CONSTRAINT "sim_phone_number_key" UNIQUE ("phone_number");

-- changeset pchuy:1777646662592-15
ALTER TABLE "user_base"
ADD CONSTRAINT "user_base_email_key" UNIQUE ("email");

-- changeset pchuy:1777646662592-16
CREATE TABLE "permission" (
    "created_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "id" UUID NOT NULL,
    "key" VARCHAR(255),
    "name" VARCHAR(255),
    CONSTRAINT "permission_pkey" PRIMARY KEY ("id")
);

-- changeset pchuy:1777646662592-17
CREATE TABLE "role" (
    "created_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "id" UUID NOT NULL,
    "key" VARCHAR(255),
    "name" VARCHAR(255),
    CONSTRAINT "role_pkey" PRIMARY KEY ("id")
);

-- changeset pchuy:1777646662592-18
CREATE TABLE "role_permissions" (
    "permission_id" UUID NOT NULL,
    "role_id" UUID NOT NULL,
    CONSTRAINT "role_permissions_pkey" PRIMARY KEY ("permission_id", "role_id")
);

-- changeset pchuy:1777646662592-19
CREATE TABLE "user_info" (
    "created_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    "id" UUID NOT NULL,
    "phone_number" VARCHAR(255),
    "username" VARCHAR(255),
    CONSTRAINT "user_info_pkey" PRIMARY KEY ("id")
);

-- changeset pchuy:1777646662592-20
CREATE TABLE "user_roles" (
    "role_id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    CONSTRAINT "user_roles_pkey" PRIMARY KEY ("role_id", "user_id")
);

-- changeset pchuy:1777646662592-21
ALTER TABLE "webhook_delivery_attempt"
ADD CONSTRAINT "fk2senyjxobbf1sxmuoas76kepw" FOREIGN KEY ("event_id") REFERENCES "webhook_event" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pchuy:1777646662592-22
ALTER TABLE "user_roles"
ADD CONSTRAINT "fkdxg65a1i8oqs3sbvmeqwqjny0" FOREIGN KEY ("user_id") REFERENCES "user_base" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pchuy:1777646662592-23
ALTER TABLE "role_permissions"
ADD CONSTRAINT "fkh0v7u4w7mttcu81o8wegayr8e" FOREIGN KEY ("permission_id") REFERENCES "permission" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pchuy:1777646662592-24
ALTER TABLE "user_info"
ADD CONSTRAINT "fkl5j35rlx7nypv8u605pud0mxa" FOREIGN KEY ("id") REFERENCES "user_base" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pchuy:1777646662592-25
ALTER TABLE "role_permissions"
ADD CONSTRAINT "fklodb7xh4a2xjv39gc3lsop95n" FOREIGN KEY ("role_id") REFERENCES "role" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pchuy:1777646662592-26
ALTER TABLE "user_roles"
ADD CONSTRAINT "fkrhfovtciq1l558cw6udg0h0d3" FOREIGN KEY ("role_id") REFERENCES "role" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pchuy:1777646662592-27
ALTER TABLE "webhook_delivery_attempt"
ADD CONSTRAINT "fk_webhook_delivery_subscription" FOREIGN KEY ("subscription_id") REFERENCES "webhook_subscription" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;