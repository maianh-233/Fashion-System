-- Shipping through a third-party provider (Lalamove first): one row per delivery
-- attempt, provider references, and de-duplication of provider webhooks.

BEGIN;

-- 1. An order can have several delivery attempts (no driver found → staff requests again,
--    or the provider replaces an order), so a shipment is no longer unique per order.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM shipments) THEN
        RAISE EXCEPTION 'Shipping migration stopped: legacy shipments rows exist';
    END IF;
END $$;

ALTER TABLE shipments DROP CONSTRAINT IF EXISTS shipments_order_id_key;

ALTER TABLE shipments
    ADD COLUMN IF NOT EXISTS attempt_no integer NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS provider_order_id varchar(64),
    ADD COLUMN IF NOT EXISTS quotation_id varchar(64),
    ADD COLUMN IF NOT EXISTS fee numeric(14,2),
    ADD COLUMN IF NOT EXISTS currency varchar(10),
    ADD COLUMN IF NOT EXISTS tracking_url text,
    -- Raw provider status (e.g. ASSIGNING_DRIVER, PICKED_UP, EXPIRED); shipping_status is the internal one.
    ADD COLUMN IF NOT EXISTS provider_status varchar(40),
    ADD COLUMN IF NOT EXISTS provider_updated_at timestamp,
    ADD COLUMN IF NOT EXISTS failure_reason text,
    ADD COLUMN IF NOT EXISTS driver_name varchar(150),
    ADD COLUMN IF NOT EXISTS driver_phone varchar(30),
    ADD COLUMN IF NOT EXISTS driver_plate varchar(30),
    ADD COLUMN IF NOT EXISTS created_by uuid,
    ADD COLUMN IF NOT EXISTS replaced_by_shipment_id uuid,
    ADD COLUMN IF NOT EXISTS raw_response jsonb;

ALTER TABLE shipments ALTER COLUMN shipping_status SET DEFAULT 'ASSIGNING_DRIVER';
ALTER TABLE shipments DROP CONSTRAINT IF EXISTS chk_shipments_status;
ALTER TABLE shipments ADD CONSTRAINT chk_shipments_status CHECK (shipping_status IN (
    'ASSIGNING_DRIVER','DRIVER_ASSIGNED','PICKED_UP','COMPLETED','FAILED','CANCELLED','REPLACED'));

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_shipments_created_by') THEN
        ALTER TABLE shipments ADD CONSTRAINT fk_shipments_created_by FOREIGN KEY (created_by) REFERENCES users(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_shipments_replaced_by') THEN
        ALTER TABLE shipments ADD CONSTRAINT fk_shipments_replaced_by
            FOREIGN KEY (replaced_by_shipment_id) REFERENCES shipments(id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_shipments_provider_order
    ON shipments(shipping_provider, provider_order_id) WHERE provider_order_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_shipments_order ON shipments(order_id, created_at DESC);

-- 2. Providers retry webhooks (Lalamove: up to 10 times in 24h); the unique event id
--    makes processing idempotent without Redis.
CREATE TABLE IF NOT EXISTS shipping_webhook_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    provider varchar(30) NOT NULL,
    event_id varchar(100) NOT NULL,
    event_type varchar(50) NOT NULL,
    provider_order_id varchar(64),
    payload jsonb NOT NULL,
    result varchar(40) NOT NULL,
    received_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_shipping_webhook_event UNIQUE (provider, event_id)
);
CREATE INDEX IF NOT EXISTS idx_shipping_webhook_events_order ON shipping_webhook_events(provider, provider_order_id);

-- 3. A gateway-paid order cancelled after a failed delivery gets a refund request handled
--    manually; the payment may belong to the parent of a split order, so the refund also
--    records which store order it was created for.
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS order_id uuid;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_refunds_order') THEN
        ALTER TABLE refunds ADD CONSTRAINT fk_refunds_order FOREIGN KEY (order_id) REFERENCES orders(id);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_refunds_order ON refunds(order_id);

COMMIT;
