-- "Báo khi có hàng": a customer subscribes to an out-of-stock variant; StockAlertScheduler notifies
-- them once it has online stock again and stamps notified_at. One pending alert per (customer, variant).

CREATE TABLE stock_alerts (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id        UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    product_variant_id UUID NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    created_at         TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    notified_at        TIMESTAMP
);

CREATE UNIQUE INDEX uq_stock_alerts_pending ON stock_alerts (customer_id, product_variant_id) WHERE notified_at IS NULL;
CREATE INDEX idx_stock_alerts_pending_variant ON stock_alerts (product_variant_id) WHERE notified_at IS NULL;
CREATE INDEX idx_stock_alerts_customer ON stock_alerts (customer_id, created_at DESC);
