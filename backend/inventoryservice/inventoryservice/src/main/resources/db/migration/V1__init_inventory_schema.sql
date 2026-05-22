-- =========================================================
-- INVENTORY SERVICE SCHEMA
-- =========================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- STORES
-- =========================================================

CREATE TABLE stores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE,
    name VARCHAR(255) NOT NULL,
    address TEXT,
    phone VARCHAR(20),

    -- tọa độ cửa hàng
    latitude  DECIMAL(9,6),
    longitude DECIMAL(9,6),

    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- =========================================================
-- STORE STAFFS
-- =========================================================

CREATE TABLE store_staffs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    store_id UUID NOT NULL,
    staff_role VARCHAR(50),
    start_date DATE,
    end_date DATE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_store_staff_store
        FOREIGN KEY (store_id)
        REFERENCES stores(id)
);

-- =========================================================
-- SUPPLIERS
-- =========================================================

CREATE TABLE suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE,
    name VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255),
    phone VARCHAR(20),
    email VARCHAR(255),
    address TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- =========================================================
-- GOODS RECEIPTS
-- =========================================================

CREATE TABLE goods_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_code VARCHAR(50) UNIQUE NOT NULL,
    supplier_id UUID,
    store_id UUID NOT NULL,
    received_by UUID,
    approved_by UUID,
    receipt_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    note TEXT,
    total_quantity INT DEFAULT 0,
    total_amount DECIMAL(14,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_goods_receipt_supplier
        FOREIGN KEY (supplier_id)
        REFERENCES suppliers(id),

    CONSTRAINT fk_goods_receipt_store
        FOREIGN KEY (store_id)
        REFERENCES stores(id)
);

-- =========================================================
-- GOODS RECEIPT ITEMS
-- =========================================================

CREATE TABLE goods_receipt_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    sku VARCHAR(100),
    product_name VARCHAR(255),
    cost_price DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    total DECIMAL(14,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_goods_receipt_item_receipt
        FOREIGN KEY (receipt_id)
        REFERENCES goods_receipts(id)
        ON DELETE CASCADE
);

-- =========================================================
-- GOODS ISSUES
-- =========================================================

CREATE TABLE goods_issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_code VARCHAR(50) UNIQUE NOT NULL,
    store_id UUID NOT NULL,
    order_id UUID,
    issued_by UUID,
    approved_by UUID,
    issue_type VARCHAR(50) NOT NULL,
    issue_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    note TEXT,
    total_quantity INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_goods_issue_store
        FOREIGN KEY (store_id)
        REFERENCES stores(id)
);

-- =========================================================
-- GOODS ISSUE ITEMS
-- =========================================================

CREATE TABLE goods_issue_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    sku VARCHAR(100),
    product_name VARCHAR(255),
    quantity INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_goods_issue_item_issue
        FOREIGN KEY (issue_id)
        REFERENCES goods_issues(id)
        ON DELETE CASCADE
);

-- =========================================================
-- INVENTORY TRANSACTIONS
-- =========================================================

CREATE TABLE inventory_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_variant_id UUID NOT NULL,
    store_id UUID NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    reference_type VARCHAR(50),
    reference_id UUID,
    quantity INT NOT NULL,
    balance_after INT NOT NULL,
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inventory_transaction_store
        FOREIGN KEY (store_id)
        REFERENCES stores(id)
);

-- =========================================================
-- INVENTORY BALANCES
-- =========================================================

CREATE TABLE inventory_balances (
    store_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    available_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    damaged_quantity INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (store_id, product_variant_id),

    CONSTRAINT fk_inventory_balance_store
        FOREIGN KEY (store_id)
        REFERENCES stores(id)
);

-- =========================================================
-- STOCK RESERVATIONS
-- =========================================================

CREATE TABLE stock_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    store_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(30) DEFAULT 'ACTIVE',
    expired_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_stock_reservation_store
        FOREIGN KEY (store_id)
        REFERENCES stores(id)
);

-- =========================================================
-- INDEXES
-- =========================================================

CREATE INDEX idx_store_staffs_store_id
    ON store_staffs(store_id);

CREATE INDEX idx_store_staffs_user_id
    ON store_staffs(user_id);

CREATE INDEX idx_goods_receipts_store_id
    ON goods_receipts(store_id);

CREATE INDEX idx_goods_receipts_supplier_id
    ON goods_receipts(supplier_id);

CREATE INDEX idx_goods_receipt_items_receipt_id
    ON goods_receipt_items(receipt_id);

CREATE INDEX idx_goods_receipt_items_variant_id
    ON goods_receipt_items(product_variant_id);

CREATE INDEX idx_goods_issues_store_id
    ON goods_issues(store_id);

CREATE INDEX idx_goods_issues_order_id
    ON goods_issues(order_id);

CREATE INDEX idx_goods_issue_items_issue_id
    ON goods_issue_items(issue_id);

CREATE INDEX idx_inventory_transactions_variant_store
    ON inventory_transactions(product_variant_id, store_id);

CREATE INDEX idx_stock_reservations_order_id
    ON stock_reservations(order_id);