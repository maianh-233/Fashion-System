-- Sequences used by CatalogIdentityService to allocate catalog and supplier codes.
-- Earlier databases were created without them, so creating a product, variant, brand,
-- category, collection or supplier failed and products seeded directly had no code.
-- Each sequence continues after the highest code already using its prefix.

BEGIN;

CREATE SEQUENCE IF NOT EXISTS catalog_brand_code_seq;
CREATE SEQUENCE IF NOT EXISTS catalog_category_code_seq;
CREATE SEQUENCE IF NOT EXISTS catalog_collection_code_seq;
CREATE SEQUENCE IF NOT EXISTS catalog_product_code_seq;
CREATE SEQUENCE IF NOT EXISTS catalog_variant_sku_seq;
CREATE SEQUENCE IF NOT EXISTS supplier_code_seq;

-- Backfill missing product codes in creation order, continuing after existing PRD codes.
WITH base AS (
    SELECT COALESCE(MAX(substring(code FROM '^PRD([0-9]+)$')::bigint), 0) AS n FROM products
), ordered AS (
    SELECT id, row_number() OVER (ORDER BY created_at NULLS LAST, id) AS rn
    FROM products
    WHERE code IS NULL OR btrim(code) = ''
)
UPDATE products p
SET code = 'PRD' || lpad((base.n + ordered.rn)::text, 6, '0')
FROM ordered, base
WHERE p.id = ordered.id;

SELECT setval(seq, GREATEST(n, 1), n > 0)
FROM (VALUES
    ('catalog_brand_code_seq',      (SELECT COALESCE(MAX(substring(code FROM '^BR([0-9]+)$')::bigint), 0) FROM brands)),
    ('catalog_category_code_seq',   (SELECT COALESCE(MAX(substring(code FROM '^CAT([0-9]+)$')::bigint), 0) FROM categories)),
    ('catalog_collection_code_seq', (SELECT COALESCE(MAX(substring(code FROM '^COL([0-9]+)$')::bigint), 0) FROM collections)),
    ('catalog_product_code_seq',    (SELECT COALESCE(MAX(substring(code FROM '^PRD([0-9]+)$')::bigint), 0) FROM products)),
    ('catalog_variant_sku_seq',     (SELECT COALESCE(MAX(substring(sku FROM '^SKU([0-9]+)$')::bigint), 0) FROM product_variants)),
    ('supplier_code_seq',           (SELECT COALESCE(MAX(substring(code FROM '^NCC([0-9]+)$')::bigint), 0) FROM suppliers))
) AS t(seq, n);

COMMIT;
