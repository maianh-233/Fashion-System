-- Sản phẩm yêu thích của khách (theo sản phẩm, không theo biến thể).
CREATE TABLE IF NOT EXISTS wishlist_items (
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    product_id  UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    PRIMARY KEY (customer_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_wishlist_items_customer_created ON wishlist_items (customer_id, created_at DESC);
