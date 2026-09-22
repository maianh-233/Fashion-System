-- PostgreSQL: replace every Product Variant with 12 fresh variants per existing Product.
-- Run this file by itself. It does not create or replace Products.
-- If any old variant is referenced by images, orders, inventory, or another table,
-- DELETE fails and PostgreSQL rolls the entire transaction back. Do not use CASCADE.

BEGIN;

-- Keep the Product set stable while deriving the new Product IDs.
LOCK TABLE products IN SHARE MODE;
LOCK TABLE product_variants IN ACCESS EXCLUSIVE MODE;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM products) THEN
        RAISE EXCEPTION 'No Products exist; refusing to delete Product Variants.';
    END IF;
END $$;

DELETE FROM product_variants;

WITH product_seed AS (
    SELECT id, row_number() OVER (ORDER BY id) AS product_number
    FROM products
),
variant_matrix(code, color, size, ordinal) AS (
    VALUES
        ('BLK-S',  'Đen',   'S',  1),
        ('BLK-M',  'Đen',   'M',  2),
        ('BLK-L',  'Đen',   'L',  3),
        ('BLK-XL', 'Đen',   'XL', 4),
        ('WHT-S',  'Trắng', 'S',  5),
        ('WHT-M',  'Trắng', 'M',  6),
        ('WHT-L',  'Trắng', 'L',  7),
        ('WHT-XL', 'Trắng', 'XL', 8),
        ('NVY-S',  'Navy',  'S',  9),
        ('NVY-M',  'Navy',  'M', 10),
        ('NVY-L',  'Navy',  'L', 11),
        ('NVY-XL', 'Navy',  'XL',12)
),
variant_rows AS (
    SELECT
        md5('fashion-variant-reset-v1-' || p.id || '-' || v.code)::uuid AS id,
        p.id AS product_id,
        'DEMO-RESET-' || replace(p.id::text, '-', '') || '-' || v.code AS sku,
        v.color,
        v.size,
        (249000 + ((p.product_number - 1) % 15) * 40000 + v.ordinal * 5000)::numeric(12,2) AS price,
        p.product_number,
        v.ordinal,
        'RESET-' || replace(p.id::text, '-', '') || '-' || lpad(v.ordinal::text, 2, '0') AS barcode
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
    true,
    current_timestamp,
    current_timestamp
FROM variant_rows;

DO $$
DECLARE
    product_count bigint;
    variant_count bigint;
BEGIN
    SELECT count(*) INTO product_count FROM products;
    SELECT count(*) INTO variant_count FROM product_variants;
    IF variant_count <> product_count * 12 OR EXISTS (
        SELECT 1 FROM product_variants v
        LEFT JOIN products p ON p.id = v.product_id
        WHERE p.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Variant reset verification failed: % Products, % Variants',
            product_count, variant_count;
    END IF;
END $$;

COMMIT;

-- Expected: variants = products * 12; both remaining columns = 0.
SELECT
    (SELECT count(*) FROM products) AS products,
    (SELECT count(*) FROM product_variants) AS variants,
    (SELECT count(*) FROM product_variants v
        LEFT JOIN products p ON p.id = v.product_id
        WHERE p.id IS NULL) AS orphan_variants,
    (SELECT count(*) FROM products p
        WHERE NOT EXISTS (SELECT 1 FROM product_variants v WHERE v.product_id = p.id))
        AS products_without_variants;
