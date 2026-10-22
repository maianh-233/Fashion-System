-- Online payment through VNPay / MoMo (sandbox first): payment deadline on the order,
-- one payments row per payment attempt, and de-duplication of gateway IPN calls.

BEGIN;

-- 1. A gateway order holds stock until it is paid; past this time the system cancels it.
--    payment_status gains AWAITING_PAYMENT (hidden from stores) and ABANDONED (cancelled unpaid).
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_due_at timestamp;

CREATE INDEX IF NOT EXISTS idx_orders_awaiting_payment
    ON orders (payment_due_at)
    WHERE payment_status = 'AWAITING_PAYMENT' AND parent_order_id IS NULL;

-- 2. Each attempt keeps the link handed to the customer and when it stops being valid.
--    payment_code is the reference sent to the gateway (vnp_TxnRef / MoMo orderId).
ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS pay_url text,
    ADD COLUMN IF NOT EXISTS expires_at timestamp,
    ADD COLUMN IF NOT EXISTS failure_reason varchar(255);

-- 3. IPN log: one row per call. event_key identifies a gateway event so a resent IPN is not applied twice.
ALTER TABLE payment_webhook_logs
    ADD COLUMN IF NOT EXISTS event_key varchar(200),
    ADD COLUMN IF NOT EXISTS payment_id uuid,
    ADD COLUMN IF NOT EXISTS signature_valid boolean,
    ADD COLUMN IF NOT EXISTS result varchar(50);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_webhook_logs_event
    ON payment_webhook_logs (provider, event_key)
    WHERE event_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payment_webhook_logs_payment ON payment_webhook_logs (payment_id);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_payment_webhook_logs_payment') THEN
        ALTER TABLE payment_webhook_logs
            ADD CONSTRAINT fk_payment_webhook_logs_payment FOREIGN KEY (payment_id) REFERENCES payments (id);
    END IF;
END $$;

COMMIT;
