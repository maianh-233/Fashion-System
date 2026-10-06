-- Explicit psql migration; this project does not automatically run Flyway.
-- Enforce invariants that application validation alone cannot protect under concurrency.
BEGIN;
SET LOCAL lock_timeout = '15s';
SET LOCAL statement_timeout = '5min';
LOCK TABLE stock_reservations IN ACCESS EXCLUSIVE MODE;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM stock_reservations WHERE quantity <= 0) THEN
        RAISE EXCEPTION 'V23: stock reservation quantity must be positive; reconcile invalid rows before retrying';
    END IF;
    IF EXISTS (SELECT 1 FROM stock_reservations WHERE status IS NULL OR status NOT IN ('ACTIVE','RELEASED','EXPIRED')) THEN
        RAISE EXCEPTION 'V23: unsupported stock reservation status; reconcile invalid rows before retrying';
    END IF;
    IF EXISTS (
        SELECT 1 FROM stock_reservations
        WHERE status = 'ACTIVE'
        GROUP BY order_id, store_id, product_variant_id
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'V23: duplicate active stock reservations; reconcile duplicates before retrying';
    END IF;
END $$;

ALTER TABLE stock_reservations ALTER COLUMN status SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'stock_reservations'::regclass
          AND conname = 'ck_stock_reservations_quantity_positive'
    ) THEN
        ALTER TABLE stock_reservations
            ADD CONSTRAINT ck_stock_reservations_quantity_positive CHECK (quantity > 0);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'stock_reservations'::regclass
          AND conname = 'ck_stock_reservations_status'
    ) THEN
        ALTER TABLE stock_reservations
            ADD CONSTRAINT ck_stock_reservations_status CHECK (status IN ('ACTIVE','RELEASED','EXPIRED'));
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_stock_reservations_active
    ON stock_reservations(order_id, store_id, product_variant_id)
    WHERE status = 'ACTIVE';

COMMIT;
