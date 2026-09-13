BEGIN;

ALTER TABLE user_tokens ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE user_tokens ADD COLUMN IF NOT EXISTS customer_id UUID;

ALTER TABLE user_tokens
  ADD CONSTRAINT fk_user_tokens_customer
  FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;

ALTER TABLE user_tokens
  ADD CONSTRAINT chk_user_tokens_single_account
  CHECK ((user_id IS NOT NULL) <> (customer_id IS NOT NULL));

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_tokens_token_hash ON user_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_user_tokens_customer_id ON user_tokens(customer_id);

COMMIT;
