ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS login_locked_until TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_customers_failed_login_attempts_non_negative'
          AND conrelid = 'customers'::regclass
    ) THEN
        ALTER TABLE customers
            ADD CONSTRAINT chk_customers_failed_login_attempts_non_negative
            CHECK (failed_login_attempts >= 0);
    END IF;
END $$;
