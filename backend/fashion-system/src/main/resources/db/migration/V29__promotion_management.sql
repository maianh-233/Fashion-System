-- Promotion management: typed promotions, lifecycle status, generated codes,
-- point redemption wallet and the loyalty tables keyed by customer.

-- 1. Loyalty tables were never moved from users to customers (V25 only added
--    customer_id through Hibernate). Point redemption debits by customer.
ALTER TABLE loyalty_accounts ADD COLUMN IF NOT EXISTS customer_id uuid;
ALTER TABLE loyalty_transactions ADD COLUMN IF NOT EXISTS customer_id uuid;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM loyalty_accounts WHERE customer_id IS NULL)
        OR EXISTS (SELECT 1 FROM loyalty_transactions WHERE customer_id IS NULL) THEN
        RAISE EXCEPTION 'Promotion migration stopped: loyalty rows without customer_id';
    END IF;
END $$;

ALTER TABLE loyalty_accounts DROP CONSTRAINT IF EXISTS loyalty_accounts_pkey;
ALTER TABLE loyalty_accounts ALTER COLUMN customer_id SET NOT NULL;
ALTER TABLE loyalty_accounts ADD CONSTRAINT loyalty_accounts_pkey PRIMARY KEY (customer_id);
ALTER TABLE loyalty_transactions ALTER COLUMN customer_id SET NOT NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'loyalty_accounts' AND column_name = 'user_id') THEN
        ALTER TABLE loyalty_accounts ALTER COLUMN user_id DROP NOT NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'loyalty_transactions' AND column_name = 'user_id') THEN
        ALTER TABLE loyalty_transactions ALTER COLUMN user_id DROP NOT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_loyalty_accounts_customer') THEN
        ALTER TABLE loyalty_accounts ADD CONSTRAINT fk_loyalty_accounts_customer
            FOREIGN KEY (customer_id) REFERENCES customers(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_loyalty_transactions_customer') THEN
        ALTER TABLE loyalty_transactions ADD CONSTRAINT fk_loyalty_transactions_customer
            FOREIGN KEY (customer_id) REFERENCES customers(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_loyalty_transactions_customer ON loyalty_transactions(customer_id);

-- 2. Promotions only target products and customer tiers.
DROP TABLE IF EXISTS promotion_brands;
DROP TABLE IF EXISTS promotion_categories;
DROP TABLE IF EXISTS promotion_collections;

-- 3. Promotion columns.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM promotions WHERE start_date IS NULL OR end_date IS NULL) THEN
        RAISE EXCEPTION 'Promotion migration stopped: promotions without start_date or end_date';
    END IF;
END $$;

ALTER TABLE promotions ALTER COLUMN discount_type TYPE varchar(20) USING discount_type::text;
DROP TYPE IF EXISTS discount_type_enum;

ALTER TABLE promotions ADD COLUMN IF NOT EXISTS promotion_type varchar(30);
ALTER TABLE promotions ADD COLUMN IF NOT EXISTS status varchar(20);
ALTER TABLE promotions ADD COLUMN IF NOT EXISTS points_required integer;
ALTER TABLE promotions ADD COLUMN IF NOT EXISTS expiry_reminded_at timestamp;

UPDATE promotions SET promotion_type = 'ORDER' WHERE promotion_type IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'promotions' AND column_name = 'active') THEN
        -- Hibernate stores timestamps in UTC (hibernate.jdbc.time_zone=UTC).
        UPDATE promotions SET status = CASE
            WHEN active IS FALSE THEN 'INACTIVE'
            WHEN end_date <= (now() AT TIME ZONE 'UTC') THEN 'EXPIRED'
            WHEN start_date > (now() AT TIME ZONE 'UTC') THEN 'SCHEDULED'
            ELSE 'ACTIVE' END
        WHERE status IS NULL;
        ALTER TABLE promotions DROP COLUMN active;
    END IF;
END $$;

UPDATE promotions SET max_discount = NULL WHERE discount_type = 'FIXED';

ALTER TABLE promotions
    ALTER COLUMN promotion_type SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN start_date SET NOT NULL,
    ALTER COLUMN end_date SET NOT NULL;

ALTER TABLE promotions DROP CONSTRAINT IF EXISTS chk_promotions_discount_type;
ALTER TABLE promotions ADD CONSTRAINT chk_promotions_discount_type
    CHECK (discount_type IN ('PERCENT', 'FIXED'));
ALTER TABLE promotions DROP CONSTRAINT IF EXISTS chk_promotions_type;
ALTER TABLE promotions ADD CONSTRAINT chk_promotions_type
    CHECK (promotion_type IN ('ORDER', 'PRODUCT', 'POINT_REDEMPTION'));
ALTER TABLE promotions DROP CONSTRAINT IF EXISTS chk_promotions_status;
ALTER TABLE promotions ADD CONSTRAINT chk_promotions_status
    CHECK (status IN ('SCHEDULED', 'ACTIVE', 'INACTIVE', 'EXPIRED'));
ALTER TABLE promotions DROP CONSTRAINT IF EXISTS chk_promotions_period;
ALTER TABLE promotions ADD CONSTRAINT chk_promotions_period CHECK (end_date > start_date);
ALTER TABLE promotions DROP CONSTRAINT IF EXISTS chk_promotions_discount_value;
ALTER TABLE promotions ADD CONSTRAINT chk_promotions_discount_value CHECK (
    discount_value > 0 AND (discount_type <> 'PERCENT' OR discount_value <= 100));
ALTER TABLE promotions DROP CONSTRAINT IF EXISTS chk_promotions_points;
ALTER TABLE promotions ADD CONSTRAINT chk_promotions_points CHECK (
    (promotion_type = 'POINT_REDEMPTION' AND points_required > 0)
    OR (promotion_type <> 'POINT_REDEMPTION' AND points_required IS NULL));

DROP INDEX IF EXISTS idx_promotions_active;
CREATE INDEX IF NOT EXISTS idx_promotions_status_start ON promotions(status, start_date);
CREATE INDEX IF NOT EXISTS idx_promotions_status_end ON promotions(status, end_date);

-- 4. Promotions a customer received by spending loyalty points.
CREATE TABLE IF NOT EXISTS customer_promotions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id uuid NOT NULL REFERENCES customers(id),
    promotion_id uuid NOT NULL REFERENCES promotions(id),
    points_spent integer NOT NULL CHECK (points_spent > 0),
    redeemed_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_customer_promotions_customer ON customer_promotions(customer_id, redeemed_at DESC);
CREATE INDEX IF NOT EXISTS idx_customer_promotions_promotion ON customer_promotions(promotion_id);
