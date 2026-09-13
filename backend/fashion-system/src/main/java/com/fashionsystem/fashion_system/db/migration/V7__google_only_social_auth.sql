BEGIN;

-- Giữ nguyên các liên kết Facebook cũ (nếu có), nhưng từ đây chỉ cho phép tạo liên kết Google.
ALTER TABLE customer_social_accounts
  DROP CONSTRAINT IF EXISTS customer_social_accounts_provider_check;

ALTER TABLE customer_social_accounts
  ADD CONSTRAINT customer_social_accounts_provider_check
  CHECK (provider = 'GOOGLE') NOT VALID;

COMMIT;
