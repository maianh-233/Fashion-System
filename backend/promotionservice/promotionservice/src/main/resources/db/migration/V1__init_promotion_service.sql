-- =========================================================
-- Promotion Service Database Migration
-- File: V1__init_promotion_service.sql
-- =========================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- ENUMS
-- =========================================================

CREATE TYPE discount_type_enum AS ENUM (
    'PERCENT',
    'FIXED'
);

-- =========================================================
-- TABLE: promotions
-- =========================================================

CREATE TABLE promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(50) NOT NULL UNIQUE,

    name VARCHAR(255) NOT NULL,

    discount_type discount_type_enum NOT NULL,

    discount_value DECIMAL(10,2) NOT NULL,

    start_date TIMESTAMP,

    end_date TIMESTAMP,

    min_order_value DECIMAL(14,2),

    max_discount DECIMAL(14,2),

    usage_limit INT,

    usage_per_user INT,

    active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP
);

-- =========================================================
-- TABLE: promotion_tiers
-- =========================================================

CREATE TABLE promotion_tiers (
    promotion_id UUID NOT NULL,
    tier_id UUID NOT NULL,

    PRIMARY KEY (promotion_id, tier_id),

    CONSTRAINT fk_promotion_tiers_promotion
        FOREIGN KEY (promotion_id)
        REFERENCES promotions(id)
        ON DELETE CASCADE
);

-- =========================================================
-- TABLE: promotion_products
-- =========================================================

CREATE TABLE promotion_products (
    promotion_id UUID NOT NULL,
    product_id UUID NOT NULL,

    PRIMARY KEY (promotion_id, product_id),

    CONSTRAINT fk_promotion_products_promotion
        FOREIGN KEY (promotion_id)
        REFERENCES promotions(id)
        ON DELETE CASCADE
);

-- =========================================================
-- TABLE: promotion_categories
-- =========================================================

CREATE TABLE promotion_categories (
    promotion_id UUID NOT NULL,
    category_id UUID NOT NULL,

    PRIMARY KEY (promotion_id, category_id),

    CONSTRAINT fk_promotion_categories_promotion
        FOREIGN KEY (promotion_id)
        REFERENCES promotions(id)
        ON DELETE CASCADE
);

-- =========================================================
-- TABLE: promotion_brands
-- =========================================================

CREATE TABLE promotion_brands (
    promotion_id UUID NOT NULL,
    brand_id UUID NOT NULL,

    PRIMARY KEY (promotion_id, brand_id),

    CONSTRAINT fk_promotion_brands_promotion
        FOREIGN KEY (promotion_id)
        REFERENCES promotions(id)
        ON DELETE CASCADE
);

-- =========================================================
-- TABLE: promotion_collections
-- =========================================================

CREATE TABLE promotion_collections (
    promotion_id UUID NOT NULL,
    collection_id UUID NOT NULL,

    PRIMARY KEY (promotion_id, collection_id),

    CONSTRAINT fk_promotion_collections_promotion
        FOREIGN KEY (promotion_id)
        REFERENCES promotions(id)
        ON DELETE CASCADE
);

-- =========================================================
-- TABLE: promotion_usages
-- =========================================================

CREATE TABLE promotion_usages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    promotion_id UUID NOT NULL,

    order_id UUID NOT NULL,

    user_id UUID,

    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_promotion_usages_promotion
        FOREIGN KEY (promotion_id)
        REFERENCES promotions(id)
        ON DELETE CASCADE
);

-- =========================================================
-- TABLE: promotion_conditions
-- =========================================================

CREATE TABLE promotion_conditions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    promotion_id UUID NOT NULL,

    condition_type VARCHAR(50) NOT NULL,

    condition_value VARCHAR(255) NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_promotion_conditions_promotion
        FOREIGN KEY (promotion_id)
        REFERENCES promotions(id)
        ON DELETE CASCADE
);

-- =========================================================
-- INDEXES
-- =========================================================

CREATE INDEX idx_promotions_code
    ON promotions(code);

CREATE INDEX idx_promotions_active
    ON promotions(active);

CREATE INDEX idx_promotions_start_date
    ON promotions(start_date);

CREATE INDEX idx_promotions_end_date
    ON promotions(end_date);

CREATE INDEX idx_promotion_usages_promotion_id
    ON promotion_usages(promotion_id);

CREATE INDEX idx_promotion_usages_order_id
    ON promotion_usages(order_id);

CREATE INDEX idx_promotion_usages_user_id
    ON promotion_usages(user_id);

CREATE INDEX idx_promotion_conditions_promotion_id
    ON promotion_conditions(promotion_id);

-- =========================================================
-- COMMENTS
-- =========================================================

COMMENT ON TABLE promotions IS 'Main promotion table';

COMMENT ON TABLE promotion_tiers IS
'Mapping promotion with membership tiers';

COMMENT ON TABLE promotion_products IS
'Mapping promotion with products';

COMMENT ON TABLE promotion_categories IS
'Mapping promotion with categories';

COMMENT ON TABLE promotion_brands IS
'Mapping promotion with brands';

COMMENT ON TABLE promotion_collections IS
'Mapping promotion with collections';

COMMENT ON TABLE promotion_usages IS
'Promotion usage history';

COMMENT ON TABLE promotion_conditions IS
'Dynamic promotion conditions';