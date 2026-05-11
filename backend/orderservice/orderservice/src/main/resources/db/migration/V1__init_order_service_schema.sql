/* =========================================================
   ORDER SERVICE DATABASE SCHEMA
   Database: order_db
   ========================================================= */

CREATE EXTENSION IF NOT EXISTS pgcrypto;

/* =========================================================
   ORDERS
   ========================================================= */

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_code VARCHAR(50) UNIQUE NOT NULL,

    user_id UUID,

    store_id UUID,

    order_type VARCHAR(20) NOT NULL,

    status VARCHAR(50) NOT NULL,

    subtotal DECIMAL(14,2) DEFAULT 0,

    discount_total DECIMAL(14,2) DEFAULT 0,

    tax DECIMAL(14,2) DEFAULT 0,

    shipping_fee DECIMAL(14,2) DEFAULT 0,

    total_amount DECIMAL(14,2) DEFAULT 0,

    payment_status VARCHAR(50) DEFAULT 'UNPAID',

    note TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP
);

/* =========================================================
   ORDER ITEMS
   ========================================================= */

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL,

    product_id UUID,

    product_variant_id UUID NOT NULL,

    product_name VARCHAR(255) NOT NULL,

    sku VARCHAR(100),

    color VARCHAR(100),

    size VARCHAR(50),

    image_url TEXT,

    price DECIMAL(12,2) NOT NULL,

    quantity INT NOT NULL,

    total DECIMAL(14,2) NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

/* =========================================================
   ORDER ADDRESSES
   ========================================================= */

CREATE TABLE order_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL UNIQUE,

    receiver_name VARCHAR(255) NOT NULL,

    receiver_phone VARCHAR(20) NOT NULL,

    province VARCHAR(100),

    district VARCHAR(100),

    ward VARCHAR(100),

    address_line TEXT,

    postal_code VARCHAR(20),

    address_type VARCHAR(20) DEFAULT 'SHIPPING',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

/* =========================================================
   ORDER STATUS HISTORIES
   ========================================================= */

CREATE TABLE order_status_histories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL,

    from_status VARCHAR(50),

    to_status VARCHAR(50) NOT NULL,

    changed_by UUID,

    note TEXT,

    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

/* =========================================================
   SHIPMENTS
   ========================================================= */

CREATE TABLE shipments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL UNIQUE,

    shipping_provider VARCHAR(100),

    tracking_code VARCHAR(100),

    shipping_status VARCHAR(50) DEFAULT 'PENDING',

    shipped_at TIMESTAMP,

    delivered_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP
);

/* =========================================================
   ORDER PROMOTIONS
   ========================================================= */

CREATE TABLE order_promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL,

    promotion_id UUID NOT NULL,

    promotion_code VARCHAR(50),

    discount_amount DECIMAL(14,2) DEFAULT 0,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

/* =========================================================
   FOREIGN KEYS
   ========================================================= */

ALTER TABLE order_items
ADD CONSTRAINT fk_order_items_order
FOREIGN KEY (order_id)
REFERENCES orders(id)
ON DELETE CASCADE;

ALTER TABLE order_addresses
ADD CONSTRAINT fk_order_addresses_order
FOREIGN KEY (order_id)
REFERENCES orders(id)
ON DELETE CASCADE;

ALTER TABLE order_status_histories
ADD CONSTRAINT fk_order_status_histories_order
FOREIGN KEY (order_id)
REFERENCES orders(id)
ON DELETE CASCADE;

ALTER TABLE shipments
ADD CONSTRAINT fk_shipments_order
FOREIGN KEY (order_id)
REFERENCES orders(id)
ON DELETE CASCADE;

ALTER TABLE order_promotions
ADD CONSTRAINT fk_order_promotions_order
FOREIGN KEY (order_id)
REFERENCES orders(id)
ON DELETE CASCADE;

/* =========================================================
   INDEXES
   ========================================================= */

CREATE INDEX idx_orders_user_id
ON orders(user_id);

CREATE INDEX idx_orders_store_id
ON orders(store_id);

CREATE INDEX idx_orders_status
ON orders(status);

CREATE INDEX idx_order_items_order_id
ON order_items(order_id);

CREATE INDEX idx_order_items_variant_id
ON order_items(product_variant_id);

CREATE INDEX idx_order_status_histories_order_id
ON order_status_histories(order_id);

CREATE INDEX idx_shipments_order_id
ON shipments(order_id);

CREATE INDEX idx_order_promotions_order_id
ON order_promotions(order_id);

/* =========================================================
   CHECK CONSTRAINTS
   ========================================================= */

ALTER TABLE order_items
ADD CONSTRAINT chk_order_items_quantity
CHECK (quantity > 0);

ALTER TABLE order_items
ADD CONSTRAINT chk_order_items_price
CHECK (price >= 0);

ALTER TABLE orders
ADD CONSTRAINT chk_orders_total_amount
CHECK (total_amount >= 0);

/* =========================================================
   COMMENTS
   ========================================================= */

COMMENT ON TABLE orders IS 'Stores customer orders';

COMMENT ON TABLE order_items IS 'Stores items belonging to an order';

COMMENT ON TABLE order_addresses IS 'Stores shipping address of an order';

COMMENT ON TABLE order_status_histories IS 'Stores order status change history';

COMMENT ON TABLE shipments IS 'Stores shipment information';

COMMENT ON TABLE order_promotions IS 'Stores promotions applied to orders';