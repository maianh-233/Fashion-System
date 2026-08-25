BEGIN;

CREATE TABLE customer_social_accounts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id UUID NOT NULL,
  provider VARCHAR(20) NOT NULL CHECK (provider IN ('GOOGLE', 'FACEBOOK')),
  provider_user_id VARCHAR(255) NOT NULL,
  provider_email VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_login_at TIMESTAMP,
  CONSTRAINT uq_customer_social_provider_subject UNIQUE (provider, provider_user_id),
  CONSTRAINT uq_customer_social_customer_provider UNIQUE (customer_id, provider),
  CONSTRAINT fk_customer_social_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX idx_customer_social_customer_id
  ON customer_social_accounts(customer_id);

CREATE TABLE revoked_tokens (
  token_id UUID PRIMARY KEY,
  account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('USER', 'CUSTOMER')),
  account_id UUID NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  revoked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_revoked_tokens_expires_at ON revoked_tokens(expires_at);

COMMIT;
