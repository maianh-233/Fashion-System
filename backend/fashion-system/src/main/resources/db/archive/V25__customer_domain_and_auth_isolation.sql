-- Separate Customer business identity from Customer website authentication.
-- This migration intentionally never updates users or internal user-token values.

DO $$
DECLARE
    invalid_phone_count integer;
    duplicate_phone_count integer;
BEGIN
    SELECT count(*) INTO invalid_phone_count
    FROM customers
    WHERE phone IS NOT NULL
      AND btrim(phone) <> ''
      AND NOT (
          regexp_replace(phone, '[^0-9]', '', 'g') ~ '^0[1-9][0-9]{8}$'
          OR regexp_replace(phone, '[^0-9]', '', 'g') ~ '^84[1-9][0-9]{8}$'
      );
    IF invalid_phone_count > 0 THEN
        RAISE EXCEPTION 'Customer migration stopped: % unsupported phone value(s)', invalid_phone_count;
    END IF;

    SELECT count(*) INTO duplicate_phone_count
    FROM (
        SELECT CASE
            WHEN regexp_replace(phone, '[^0-9]', '', 'g') ~ '^0[1-9][0-9]{8}$'
                THEN '+84' || substring(regexp_replace(phone, '[^0-9]', '', 'g') FROM 2)
            WHEN regexp_replace(phone, '[^0-9]', '', 'g') ~ '^84[1-9][0-9]{8}$'
                THEN '+' || regexp_replace(phone, '[^0-9]', '', 'g')
        END normalized
        FROM customers
        WHERE phone IS NOT NULL AND btrim(phone) <> ''
        GROUP BY normalized
        HAVING count(*) > 1
    ) duplicates;
    IF duplicate_phone_count > 0 THEN
        RAISE EXCEPTION 'Customer migration stopped: % ambiguous normalized phone(s)', duplicate_phone_count;
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS customer_code_seq START WITH 1;

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS customer_code varchar(30),
    ADD COLUMN IF NOT EXISTS normalized_phone varchar(20),
    ADD COLUMN IF NOT EXISTS source varchar(20),
    ADD COLUMN IF NOT EXISTS membership_status varchar(20),
    ADD COLUMN IF NOT EXISTS origin_store_id uuid,
    ADD COLUMN IF NOT EXISTS note text;

UPDATE customers
SET normalized_phone = CASE
        WHEN phone IS NULL OR btrim(phone) = '' THEN NULL
        WHEN regexp_replace(phone, '[^0-9]', '', 'g') ~ '^0[1-9][0-9]{8}$'
            THEN '+84' || substring(regexp_replace(phone, '[^0-9]', '', 'g') FROM 2)
        ELSE '+' || regexp_replace(phone, '[^0-9]', '', 'g')
    END,
    source = COALESCE(source, 'WEBSITE'),
    membership_status = COALESCE(membership_status, 'MEMBER'),
    customer_code = COALESCE(customer_code, 'CUS' || lpad(nextval('customer_code_seq')::text, 6, '0'));

ALTER TABLE customers
    ALTER COLUMN customer_code SET NOT NULL,
    ALTER COLUMN source SET NOT NULL,
    ALTER COLUMN membership_status SET NOT NULL,
    ALTER COLUMN active SET DEFAULT true;

CREATE UNIQUE INDEX IF NOT EXISTS uq_customers_customer_code ON customers(customer_code);
CREATE UNIQUE INDEX IF NOT EXISTS uq_customers_normalized_phone
    ON customers(normalized_phone) WHERE normalized_phone IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_customers_email_normalized
    ON customers(lower(email)) WHERE email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_customers_origin_store ON customers(origin_store_id);
CREATE INDEX IF NOT EXISTS idx_customers_source ON customers(source);
CREATE INDEX IF NOT EXISTS idx_customers_active ON customers(active);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_customers_source' AND conrelid='customers'::regclass) THEN
        ALTER TABLE customers ADD CONSTRAINT ck_customers_source CHECK (source IN ('STORE','WEBSITE'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_customers_membership' AND conrelid='customers'::regclass) THEN
        ALTER TABLE customers ADD CONSTRAINT ck_customers_membership CHECK (membership_status IN ('MEMBER'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_customers_origin' AND conrelid='customers'::regclass) THEN
        ALTER TABLE customers ADD CONSTRAINT ck_customers_origin CHECK (
            (source='STORE' AND origin_store_id IS NOT NULL) OR source='WEBSITE'
        );
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_customers_origin_store' AND conrelid='customers'::regclass) THEN
        ALTER TABLE customers ADD CONSTRAINT fk_customers_origin_store
            FOREIGN KEY(origin_store_id) REFERENCES stores(id);
    END IF;
END $$;

CREATE TABLE customer_accounts(
    id uuid PRIMARY KEY,
    customer_id uuid NOT NULL UNIQUE REFERENCES customers(id) ON DELETE CASCADE,
    username varchar(50) NOT NULL UNIQUE,
    login_email varchar(255),
    password_hash text,
    failed_login_attempts integer NOT NULL DEFAULT 0 CHECK (failed_login_attempts >= 0),
    login_locked_until timestamp,
    created_at timestamp NOT NULL DEFAULT current_timestamp,
    updated_at timestamp NOT NULL DEFAULT current_timestamp
);
CREATE UNIQUE INDEX uq_customer_accounts_login_email
    ON customer_accounts(lower(login_email)) WHERE login_email IS NOT NULL;

INSERT INTO customer_accounts(
    id, customer_id, username, login_email, password_hash,
    failed_login_attempts, login_locked_until, created_at, updated_at
)
SELECT id, id, lower(username), lower(email), password_hash,
       COALESCE(failed_login_attempts, 0), login_locked_until,
       COALESCE(created_at, current_timestamp), COALESCE(updated_at, current_timestamp)
FROM customers;

CREATE TABLE customer_refresh_tokens(
    id uuid PRIMARY KEY,
    customer_account_id uuid NOT NULL REFERENCES customer_accounts(id) ON DELETE CASCADE,
    token_hash varchar(64) NOT NULL UNIQUE,
    refresh_token_family uuid NOT NULL,
    parent_token_id uuid REFERENCES customer_refresh_tokens(id),
    expires_at timestamp NOT NULL,
    revoked_at timestamp,
    device varchar(100),
    ip_address varchar(50),
    user_agent text,
    created_at timestamp NOT NULL DEFAULT current_timestamp
);
CREATE INDEX idx_customer_refresh_tokens_account ON customer_refresh_tokens(customer_account_id);
CREATE INDEX idx_customer_refresh_tokens_family ON customer_refresh_tokens(refresh_token_family);

INSERT INTO customer_refresh_tokens(
    id, customer_account_id, token_hash, refresh_token_family, parent_token_id,
    expires_at, revoked_at, device, ip_address, user_agent, created_at
)
SELECT t.id, a.id, t.token_hash, COALESCE(t.refresh_token_family, t.id), NULL,
       t.expires_at, t.revoked_at, t.device, t.ip_address, t.user_agent,
       COALESCE(t.created_at, current_timestamp)
FROM user_tokens t
JOIN customer_accounts a ON a.customer_id=t.customer_id
WHERE t.customer_id IS NOT NULL;

DELETE FROM user_tokens WHERE customer_id IS NOT NULL;
ALTER TABLE user_tokens DROP COLUMN IF EXISTS customer_id;

CREATE TABLE customer_password_reset_challenges(
    id uuid PRIMARY KEY,
    customer_account_id uuid NOT NULL REFERENCES customer_accounts(id) ON DELETE CASCADE,
    email varchar(255) NOT NULL,
    otp_hash text NOT NULL,
    reset_token_hash varchar(64),
    expires_at timestamp NOT NULL,
    reset_token_expires_at timestamp,
    verified_at timestamp,
    used_at timestamp,
    failed_attempts integer NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0),
    created_at timestamp NOT NULL DEFAULT current_timestamp
);
CREATE INDEX idx_customer_password_reset_account_created
    ON customer_password_reset_challenges(customer_account_id, created_at DESC);
CREATE UNIQUE INDEX uq_customer_password_reset_token
    ON customer_password_reset_challenges(reset_token_hash) WHERE reset_token_hash IS NOT NULL;

INSERT INTO customer_password_reset_challenges(
    id, customer_account_id, email, otp_hash, reset_token_hash, expires_at,
    reset_token_expires_at, verified_at, used_at, failed_attempts, created_at
)
SELECT p.id, a.id, lower(p.email), p.otp_hash, p.reset_token_hash, p.expires_at,
       p.reset_token_expires_at, p.verified_at, p.used_at, p.failed_attempts, p.created_at
FROM password_reset_otps p
JOIN customer_accounts a ON a.customer_id=p.account_id
WHERE p.account_type='CUSTOMER';

DELETE FROM password_reset_otps WHERE account_type='CUSTOMER';

ALTER TABLE customer_addresses
    ADD COLUMN IF NOT EXISTS management_source varchar(20) NOT NULL DEFAULT 'WEB';
ALTER TABLE customer_addresses ALTER COLUMN user_id DROP NOT NULL;
DROP INDEX IF EXISTS uq_customer_addresses_default_user;
CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_addresses_default_customer
    ON customer_addresses(customer_id) WHERE is_default=true;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_customer_addresses_source' AND conrelid='customer_addresses'::regclass) THEN
        ALTER TABLE customer_addresses ADD CONSTRAINT ck_customer_addresses_source
            CHECK (management_source IN ('STORE','WEB'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_customer_addresses_customer' AND conrelid='customer_addresses'::regclass) THEN
        ALTER TABLE customer_addresses ADD CONSTRAINT fk_customer_addresses_customer
            FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_customer_social_customer' AND conrelid='customer_social_accounts'::regclass) THEN
        ALTER TABLE customer_social_accounts ADD CONSTRAINT fk_customer_social_customer
            FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_customer_tier_assignment_customer' AND conrelid='customer_tier_assignments'::regclass) THEN
        ALTER TABLE customer_tier_assignments ADD CONSTRAINT fk_customer_tier_assignment_customer
            FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_customer_tier_assignment_tier' AND conrelid='customer_tier_assignments'::regclass) THEN
        ALTER TABLE customer_tier_assignments ADD CONSTRAINT fk_customer_tier_assignment_tier
            FOREIGN KEY(tier_id) REFERENCES customer_tiers(id);
    END IF;
END $$;

INSERT INTO customer_tiers(id,code,name,min_total_spent,discount_percent,created_at,updated_at)
VALUES ('00000000-0000-0000-0000-000000000001','REGULAR','Regular',0,0,current_timestamp,current_timestamp)
ON CONFLICT(code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM customer_tier_assignments
        WHERE expires_at IS NULL GROUP BY customer_id HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Customer migration stopped: multiple current tier assignments';
    END IF;
END $$;

INSERT INTO customer_tier_assignments(id,customer_id,tier_id,assigned_at,note)
SELECT (
    substr(md5(c.id::text || ':regular'),1,8) || '-' ||
    substr(md5(c.id::text || ':regular'),9,4) || '-' ||
    substr(md5(c.id::text || ':regular'),13,4) || '-' ||
    substr(md5(c.id::text || ':regular'),17,4) || '-' ||
    substr(md5(c.id::text || ':regular'),21,12)
)::uuid,
c.id, t.id, COALESCE(c.created_at,current_timestamp), 'Default REGULAR tier'
FROM customers c
JOIN customer_tiers t ON t.code='REGULAR'
WHERE NOT EXISTS (
    SELECT 1 FROM customer_tier_assignments a
    WHERE a.customer_id=c.id AND a.expires_at IS NULL
);
CREATE UNIQUE INDEX uq_customer_tier_assignments_current
    ON customer_tier_assignments(customer_id) WHERE expires_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_customer_tier_assignments_tier
    ON customer_tier_assignments(tier_id);

ALTER TABLE customers
    DROP COLUMN IF EXISTS username,
    DROP COLUMN IF EXISTS password_hash,
    DROP COLUMN IF EXISTS locked,
    DROP COLUMN IF EXISTS failed_login_attempts,
    DROP COLUMN IF EXISTS login_locked_until;
