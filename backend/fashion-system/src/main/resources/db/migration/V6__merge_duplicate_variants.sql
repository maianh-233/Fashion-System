-- Merge product variants that share (product_id, color, size) and forbid them from now on. Colour and size
-- compare like ProductVariantService.ensureCombinationAvailable: trimmed, case-insensitive, missing = ''.
-- The storefront picks the first variant matching a colour/size, so stock, carts or alerts attached to
-- a duplicate were unreachable. The demo seed had 900 such pairs (DEMO-P… and DEMO-V2-…).
--
-- Per group the newest variant (created_at, then id) is kept. Every reference is moved to it;
-- inventory balances of the same store are summed. Order lines keep their own sku/name snapshot.
-- Databases without duplicates only gain the unique index.

CREATE TEMP TABLE variant_merge ON COMMIT DROP AS
SELECT id AS duplicate_id, keeper_id
FROM (
    SELECT id,
           first_value(id) OVER (PARTITION BY product_id, lower(coalesce(trim(color), '')), lower(coalesce(trim(size), ''))
                                 ORDER BY created_at DESC NULLS LAST, id DESC) AS keeper_id
    FROM product_variants
) ranked
WHERE id <> keeper_id;

-- Stock: add each duplicate's balance into the keeper's row of the same store, or move the row.
UPDATE inventory_balances k
SET available_quantity = k.available_quantity + d.available_quantity,
    online_quantity    = k.online_quantity + d.online_quantity,
    reserved_quantity  = k.reserved_quantity + d.reserved_quantity,
    damaged_quantity   = k.damaged_quantity + d.damaged_quantity,
    version            = k.version + 1,
    updated_at         = now() AT TIME ZONE 'UTC'
FROM (SELECT m.keeper_id, b.store_id,
             sum(b.available_quantity) AS available_quantity, sum(b.online_quantity) AS online_quantity,
             sum(b.reserved_quantity) AS reserved_quantity, sum(b.damaged_quantity) AS damaged_quantity
      FROM inventory_balances b JOIN variant_merge m ON m.duplicate_id = b.product_variant_id
      GROUP BY m.keeper_id, b.store_id) d
WHERE k.product_variant_id = d.keeper_id AND k.store_id = d.store_id;

DELETE FROM inventory_balances b
USING variant_merge m
WHERE b.product_variant_id = m.duplicate_id
  AND EXISTS (SELECT 1 FROM inventory_balances k WHERE k.product_variant_id = m.keeper_id AND k.store_id = b.store_id);

INSERT INTO inventory_balances (store_id, product_variant_id, available_quantity, online_quantity,
                                reserved_quantity, damaged_quantity, version, updated_at)
SELECT b.store_id, m.keeper_id, sum(b.available_quantity), sum(b.online_quantity),
       sum(b.reserved_quantity), sum(b.damaged_quantity), 0, now() AT TIME ZONE 'UTC'
FROM inventory_balances b JOIN variant_merge m ON m.duplicate_id = b.product_variant_id
GROUP BY b.store_id, m.keeper_id;

DELETE FROM inventory_balances b USING variant_merge m WHERE b.product_variant_id = m.duplicate_id;

-- Carts: one line per (customer, variant); add quantities (capped at 99) when both lines exist.
UPDATE cart_items k
SET quantity = least(99, k.quantity + d.quantity), updated_at = now() AT TIME ZONE 'UTC'
FROM (SELECT c.customer_id, m.keeper_id, sum(c.quantity) AS quantity
      FROM cart_items c JOIN variant_merge m ON m.duplicate_id = c.product_variant_id
      GROUP BY c.customer_id, m.keeper_id) d
WHERE k.customer_id = d.customer_id AND k.product_variant_id = d.keeper_id;

DELETE FROM cart_items c
USING variant_merge m
WHERE c.product_variant_id = m.duplicate_id
  AND EXISTS (SELECT 1 FROM cart_items k WHERE k.customer_id = c.customer_id AND k.product_variant_id = m.keeper_id);

UPDATE cart_items c SET product_variant_id = m.keeper_id
FROM variant_merge m WHERE c.product_variant_id = m.duplicate_id;

-- Pending back-in-stock alerts: keep one per (customer, keeper).
DELETE FROM stock_alerts a
USING variant_merge m
WHERE a.product_variant_id = m.duplicate_id AND a.notified_at IS NULL
  AND EXISTS (SELECT 1 FROM stock_alerts k WHERE k.customer_id = a.customer_id
                                             AND k.product_variant_id = m.keeper_id AND k.notified_at IS NULL);

UPDATE stock_alerts a SET product_variant_id = m.keeper_id
FROM variant_merge m WHERE a.product_variant_id = m.duplicate_id;

-- Plain references: history, documents, reservations, images, reviews.
UPDATE order_items x SET product_variant_id = m.keeper_id FROM variant_merge m WHERE x.product_variant_id = m.duplicate_id;
UPDATE inventory_transactions x SET product_variant_id = m.keeper_id FROM variant_merge m WHERE x.product_variant_id = m.duplicate_id;
UPDATE goods_receipt_items x SET product_variant_id = m.keeper_id FROM variant_merge m WHERE x.product_variant_id = m.duplicate_id;
UPDATE goods_issue_items x SET product_variant_id = m.keeper_id FROM variant_merge m WHERE x.product_variant_id = m.duplicate_id;
UPDATE stock_reservations x SET product_variant_id = m.keeper_id FROM variant_merge m WHERE x.product_variant_id = m.duplicate_id;
UPDATE product_images x SET product_variant_id = m.keeper_id FROM variant_merge m WHERE x.product_variant_id = m.duplicate_id;
UPDATE product_reviews x SET product_variant_id = m.keeper_id FROM variant_merge m WHERE x.product_variant_id = m.duplicate_id;

DELETE FROM product_variants v USING variant_merge m WHERE v.id = m.duplicate_id;

-- One variant per colour/size of a product, same normalisation as the application check.
CREATE UNIQUE INDEX uq_product_variants_product_color_size
    ON product_variants (product_id, lower(coalesce(trim(color), '')), lower(coalesce(trim(size), '')));
