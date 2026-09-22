-- ============================================================================
-- LARGE PRODUCT VARIANT DEMO DATA (POSTGRESQL)
--
-- Fills up to 12 deterministic text-only Variants for every existing Product.
-- Safe to run repeatedly: existing SKU and color/size combinations stay untouched.
-- Does not create Product images or Inventory balances.
-- ============================================================================

BEGIN;

WITH product_seed AS (
    SELECT id, row_number() OVER (ORDER BY id) AS product_number
    FROM products
),
variant_matrix(code, color, size, ordinal) AS (
    VALUES
        ('BLK-S',  'Đen',  'S',  1),
        ('BLK-M',  'Đen',  'M',  2),
        ('BLK-L',  'Đen',  'L',  3),
        ('BLK-XL', 'Đen',  'XL', 4),
        ('WHT-S',  'Trắng','S',  5),
        ('WHT-M',  'Trắng','M',  6),
        ('WHT-L',  'Trắng','L',  7),
        ('WHT-XL', 'Trắng','XL', 8),
        ('NVY-S',  'Navy', 'S',  9),
        ('NVY-M',  'Navy', 'M', 10),
        ('NVY-L',  'Navy', 'L', 11),
        ('NVY-XL', 'Navy', 'XL',12)
),
variant_rows AS (
    SELECT
        md5('fashion-variant-seed-v2-' || p.id || '-' || v.code)::uuid AS id,
        p.id AS product_id,
        'DEMO-V2-' || replace(p.id::text, '-', '') || '-' || v.code AS sku,
        v.color,
        v.size,
        (249000 + ((p.product_number - 1) % 15) * 40000 + v.ordinal * 5000)::numeric(12,2) AS price,
        p.product_number,
        v.ordinal,
        ('DEMO2-' || replace(p.id::text, '-', '') || '-' || lpad(v.ordinal::text, 2, '0')) AS barcode
    FROM product_seed p
    CROSS JOIN variant_matrix v
)
INSERT INTO product_variants (
    id, product_id, sku, color, size, price, sale_price,
    weight, barcode, active, created_at, updated_at
)
SELECT
    id,
    product_id,
    sku,
    color,
    size,
    price,
    CASE WHEN (product_number + ordinal) % 3 = 0 THEN price - 30000 ELSE NULL END,
    (180 + ((product_number - 1) % 8) * 35 + ordinal * 4)::numeric(8,2),
    barcode,
    (product_number + ordinal) % 23 <> 0,
    current_timestamp,
    current_timestamp
FROM variant_rows
WHERE NOT EXISTS (
    SELECT 1 FROM product_variants existing
    WHERE existing.product_id = variant_rows.product_id
      AND lower(coalesce(nullif(btrim(existing.color), ''), '')) = lower(btrim(variant_rows.color))
      AND lower(coalesce(nullif(btrim(existing.size), ''), '')) = lower(btrim(variant_rows.size))
)
ON CONFLICT DO NOTHING;

COMMIT;

-- Result summary. products_without_variants should be 0 when products exist.
SELECT
    (SELECT count(*) FROM products) AS products,
    (SELECT count(*) FROM product_variants) AS all_variants,
    (SELECT count(*) FROM product_variants WHERE sku LIKE 'DEMO-V2-%') AS seeded_variants,
    (SELECT count(*) FROM products p WHERE NOT EXISTS (
        SELECT 1 FROM product_variants v WHERE v.product_id = p.id
    )) AS products_without_variants;
