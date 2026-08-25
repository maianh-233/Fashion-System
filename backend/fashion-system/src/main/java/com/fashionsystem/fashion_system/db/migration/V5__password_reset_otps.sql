BEGIN;

-- account_id là khóa đa hình tới users hoặc customers tùy account_type.
CREATE TABLE password_reset_otps (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('EMPLOYEE', 'CUSTOMER')),
  account_id UUID NOT NULL,
  email VARCHAR(255) NOT NULL,
  otp_hash TEXT NOT NULL,
  reset_token_hash VARCHAR(64),
  expires_at TIMESTAMP NOT NULL,
  reset_token_expires_at TIMESTAMP,
  verified_at TIMESTAMP,
  used_at TIMESTAMP,
  failed_attempts INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_password_reset_otps_lookup
  ON password_reset_otps(email, account_type, created_at DESC);

CREATE UNIQUE INDEX uq_password_reset_otps_reset_token
  ON password_reset_otps(reset_token_hash) WHERE reset_token_hash IS NOT NULL;

COMMIT;
