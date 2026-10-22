-- Offline (counter) orders: per-line promotion discounts and single-use
-- point-redemption vouchers.

BEGIN;

-- 1. order_items.price is the original unit price; a PRODUCT promotion discount is
--    stored on the line so staff and receipts show "original price / after discount".
--    total = price * quantity - discount_amount.
ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS discount_amount numeric(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS promotion_id uuid,
    ADD COLUMN IF NOT EXISTS promotion_code varchar(50);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_order_items_promotion') THEN
        ALTER TABLE order_items ADD CONSTRAINT fk_order_items_promotion
            FOREIGN KEY (promotion_id) REFERENCES promotions(id);
    END IF;
END $$;

ALTER TABLE order_items DROP CONSTRAINT IF EXISTS chk_order_items_discount;
ALTER TABLE order_items ADD CONSTRAINT chk_order_items_discount
    CHECK (discount_amount >= 0 AND total >= 0);

-- 2. A voucher bought with loyalty points is used at most once. Cancelling or
--    returning the order does not give it back (business rule).
ALTER TABLE customer_promotions
    ADD COLUMN IF NOT EXISTS used_order_id uuid,
    ADD COLUMN IF NOT EXISTS used_at timestamp;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_customer_promotions_used_order') THEN
        ALTER TABLE customer_promotions ADD CONSTRAINT fk_customer_promotions_used_order
            FOREIGN KEY (used_order_id) REFERENCES orders(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_customer_promotions_unused
    ON customer_promotions(customer_id, promotion_id) WHERE used_order_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_promotion_usages_promotion_customer
    ON promotion_usages(promotion_id, customer_id);

COMMIT;
