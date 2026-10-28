-- Server-side shopping cart for online customers: one row per (customer, variant).
-- Prices are never stored here; the cart is priced from the catalog every time it is read.

BEGIN;

CREATE TABLE IF NOT EXISTS cart_items (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id        uuid NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    product_variant_id uuid NOT NULL REFERENCES product_variants (id) ON DELETE CASCADE,
    quantity           integer NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    -- Ticked for checkout; unticked lines stay in the cart for later.
    selected           boolean NOT NULL DEFAULT true,
    created_at         timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cart_items_customer_variant UNIQUE (customer_id, product_variant_id)
);

CREATE INDEX IF NOT EXISTS idx_cart_items_customer ON cart_items (customer_id, created_at);

COMMIT;
