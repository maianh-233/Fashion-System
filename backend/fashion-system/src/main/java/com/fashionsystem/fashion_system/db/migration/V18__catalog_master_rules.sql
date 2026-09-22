-- Catalog identities and soft-delete fields. Apply after V17.
CREATE SEQUENCE IF NOT EXISTS catalog_brand_code_seq;
CREATE SEQUENCE IF NOT EXISTS catalog_category_code_seq;
CREATE SEQUENCE IF NOT EXISTS catalog_collection_code_seq;
CREATE SEQUENCE IF NOT EXISTS catalog_product_code_seq;
CREATE SEQUENCE IF NOT EXISTS catalog_variant_sku_seq;

SELECT setval('catalog_brand_code_seq', GREATEST(1, COALESCE((SELECT MAX(substring(code FROM 3)::bigint) FROM brands WHERE code ~ '^BR[0-9]+$'), 0) + 1), false);
SELECT setval('catalog_category_code_seq', GREATEST(1, COALESCE((SELECT MAX(substring(code FROM 4)::bigint) FROM categories WHERE code ~ '^CAT[0-9]+$'), 0) + 1), false);
SELECT setval('catalog_collection_code_seq', GREATEST(1, COALESCE((SELECT MAX(substring(code FROM 4)::bigint) FROM collections WHERE code ~ '^COL[0-9]+$'), 0) + 1), false);
SELECT setval('catalog_product_code_seq', 1, false);
SELECT setval('catalog_variant_sku_seq', GREATEST(1, COALESCE((SELECT MAX(substring(sku FROM 4)::bigint) FROM product_variants WHERE sku ~ '^SKU[0-9]+$'), 0) + 1), false);

-- Existing catalog tables can already contain rows. Add the columns as nullable
-- first, then backfill them before enforcing the JPA NOT NULL contract.
ALTER TABLE categories ADD COLUMN IF NOT EXISTS active BOOLEAN;
UPDATE categories SET active = TRUE WHERE active IS NULL;
ALTER TABLE categories ALTER COLUMN active SET DEFAULT TRUE;
ALTER TABLE categories ALTER COLUMN active SET NOT NULL;

ALTER TABLE product_tags ADD COLUMN IF NOT EXISTS active BOOLEAN;
UPDATE product_tags SET active = TRUE WHERE active IS NULL;
ALTER TABLE product_tags ALTER COLUMN active SET DEFAULT TRUE;
ALTER TABLE product_tags ALTER COLUMN active SET NOT NULL;
ALTER TABLE products ADD COLUMN IF NOT EXISTS code VARCHAR(100);
ALTER TABLE brands ADD COLUMN IF NOT EXISTS logo_public_id VARCHAR(255);
ALTER TABLE collections ADD COLUMN IF NOT EXISTS image_public_id VARCHAR(255);
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_public_id VARCHAR(255);
CREATE UNIQUE INDEX IF NOT EXISTS ux_products_code ON products(code);
CREATE UNIQUE INDEX IF NOT EXISTS ux_tags_normalized_name ON product_tags(lower(btrim(name)));
CREATE UNIQUE INDEX IF NOT EXISTS ux_variants_product_color_size
    ON product_variants(product_id, lower(coalesce(nullif(btrim(color), ''), '')), lower(coalesce(nullif(btrim(size), ''), '')));
CREATE INDEX IF NOT EXISTS idx_categories_active ON categories(active);
CREATE INDEX IF NOT EXISTS idx_tags_active ON product_tags(active);
