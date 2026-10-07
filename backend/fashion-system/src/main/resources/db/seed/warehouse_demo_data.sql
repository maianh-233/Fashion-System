-- Development/demo data for the supplier and warehouse modules.
-- PostgreSQL only. Apply V22 and V23 before running this file.
--
-- Run with:
--   psql -v ON_ERROR_STOP=1 -d <database_name> -f src/main/resources/db/seed/warehouse_demo_data.sql
--
-- The script never deletes data. It uses one active store and at most six active
-- variants that have no balance at that store. On later runs it reuses only the
-- variants previously selected by this seed, making the script repeatable.

BEGIN;
SET LOCAL lock_timeout = '15s';
SET LOCAL statement_timeout = '2min';
SELECT pg_advisory_xact_lock(hashtext('fashion-system:warehouse-demo-data'));

CREATE TEMP TABLE warehouse_demo_context ON COMMIT DROP AS
SELECT
    s.id AS store_id,
    (SELECT u.id FROM users u ORDER BY u.id LIMIT 1) AS actor_id
FROM stores s
WHERE COALESCE(s.active, FALSE)
ORDER BY s.code, s.id
LIMIT 1;

CREATE TEMP TABLE warehouse_demo_variants ON COMMIT DROP AS
WITH candidates AS (
    SELECT
        v.id AS variant_id,
        v.product_id,
        v.sku,
        p.name AS product_name,
        EXISTS (
            SELECT 1
            FROM goods_receipt_items seeded_item
            WHERE seeded_item.receipt_id = 'da000000-0000-0000-0000-000000000001'::uuid
              AND seeded_item.product_variant_id = v.id
        ) AS previously_seeded
    FROM warehouse_demo_context ctx
    JOIN product_variants v ON COALESCE(v.active, FALSE)
    JOIN products p ON p.id = v.product_id
    WHERE NOT EXISTS (
        SELECT 1
        FROM inventory_balances balance
        WHERE balance.store_id = ctx.store_id
          AND balance.product_variant_id = v.id
    )
    OR EXISTS (
        SELECT 1
        FROM goods_receipt_items seeded_item
        WHERE seeded_item.receipt_id = 'da000000-0000-0000-0000-000000000001'::uuid
          AND seeded_item.product_variant_id = v.id
    )
), selected AS (
    SELECT *
    FROM candidates
    ORDER BY previously_seeded DESC, sku, variant_id
    LIMIT 6
)
SELECT
    row_number() OVER (ORDER BY previously_seeded DESC, sku, variant_id) AS rn,
    variant_id,
    product_id,
    sku,
    product_name
FROM selected;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM warehouse_demo_context) THEN
        RAISE EXCEPTION 'Warehouse demo seed requires at least one active store';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM warehouse_demo_variants) THEN
        RAISE EXCEPTION 'Warehouse demo seed requires an active product variant without inventory at the selected store';
    END IF;
END $$;

INSERT INTO suppliers (
    id, code, name, contact_name, phone, email, address, status, created_at, updated_at
)
VALUES
    ('de000000-0000-0000-0000-000000000001', 'NCC-DEMO-001',
     'Công ty Dệt May Ánh Dương', 'Nguyễn An Nhiên', '0900001001',
     'anhduong@example.com', '12 Đường Mẫu, Quận 1, TP. Hồ Chí Minh', 'ACTIVE',
     CURRENT_TIMESTAMP - INTERVAL '120 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),
    ('de000000-0000-0000-0000-000000000002', 'NCC-DEMO-002',
     'Xưởng Vải Mộc Lam', 'Trần Minh Khuê', '0900001002',
     'moclam@example.com', '28 Phố Minh Họa, Hải Châu, Đà Nẵng', 'ACTIVE',
     CURRENT_TIMESTAMP - INTERVAL '110 days', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    ('de000000-0000-0000-0000-000000000003', 'NCC-DEMO-003',
     'Phụ liệu Thời Trang Sao Mai', 'Lê Gia Hân', '0900001003',
     'saomai@example.com', '55 Đường Thử Nghiệm, Ninh Kiều, Cần Thơ', 'ACTIVE',
     CURRENT_TIMESTAMP - INTERVAL '90 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    ('de000000-0000-0000-0000-000000000004', 'NCC-DEMO-004',
     'Công ty Bao Bì Hộp Việt', 'Phạm Thiên Long', '0900001004',
     'hopviet@example.com', '76 Đại lộ Demo, Thủ Đức, TP. Hồ Chí Minh', 'ACTIVE',
     CURRENT_TIMESTAMP - INTERVAL '75 days', CURRENT_TIMESTAMP - INTERVAL '7 days'),
    ('de000000-0000-0000-0000-000000000005', 'NCC-DEMO-005',
     'Nhà cung cấp Sợi Ngân Hà', 'Võ Bảo Châu', '0900001005',
     'nganha@example.com', '19 Đường Dữ Liệu, Long Biên, Hà Nội', 'INACTIVE',
     CURRENT_TIMESTAMP - INTERVAL '150 days', CURRENT_TIMESTAMP - INTERVAL '30 days')
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    contact_name = EXCLUDED.contact_name,
    phone = EXCLUDED.phone,
    email = EXCLUDED.email,
    address = EXCLUDED.address,
    status = EXCLUDED.status,
    updated_at = EXCLUDED.updated_at;

INSERT INTO goods_receipts (
    id, receipt_code, supplier_id, store_id, received_by, approved_by,
    receipt_date, status, note, total_quantity, total_amount, created_at,
    updated_at, confirmed_at, completed_at, completed_by
)
SELECT
    receipt.id,
    receipt.code,
    supplier.id,
    ctx.store_id,
    ctx.actor_id,
    CASE WHEN receipt.status IN ('CONFIRMED', 'COMPLETED') THEN ctx.actor_id END,
    receipt.created_at,
    receipt.status,
    receipt.note,
    (SELECT count(*)::integer * receipt.quantity_per_variant FROM warehouse_demo_variants),
    (SELECT COALESCE(sum((100000 + variant.rn * 25000) * receipt.quantity_per_variant), 0)
       FROM warehouse_demo_variants variant),
    receipt.created_at,
    receipt.updated_at,
    CASE WHEN receipt.status IN ('CONFIRMED', 'COMPLETED') THEN receipt.created_at + INTERVAL '1 hour' END,
    CASE WHEN receipt.status = 'COMPLETED' THEN receipt.created_at + INTERVAL '2 hours' END,
    CASE WHEN receipt.status = 'COMPLETED' THEN ctx.actor_id END
FROM warehouse_demo_context ctx
CROSS JOIN (VALUES
    ('da000000-0000-0000-0000-000000000001'::uuid, 'DEMO-PN-001', 'NCC-DEMO-001', 'COMPLETED', 60,
     'Phiếu nhập demo đã hoàn tất: phân bổ hàng cho cả kênh cửa hàng và online.', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '20 days' + INTERVAL '2 hours'),
    ('da000000-0000-0000-0000-000000000002'::uuid, 'DEMO-PN-002', 'NCC-DEMO-002', 'CONFIRMED', 12,
     'Phiếu nhập demo đã xác nhận, đang chờ hoàn tất nhập kho.', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '1 hour'),
    ('da000000-0000-0000-0000-000000000003'::uuid, 'DEMO-PN-003', 'NCC-DEMO-003', 'PENDING_CONFIRMATION', 8,
     'Phiếu nhập demo đang chờ xác nhận.', CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '23 hours'),
    ('da000000-0000-0000-0000-000000000004'::uuid, 'DEMO-PN-004', 'NCC-DEMO-004', 'DRAFT', 6,
     'Phiếu nhập demo đang soạn.', CURRENT_TIMESTAMP - INTERVAL '6 hours', CURRENT_TIMESTAMP - INTERVAL '5 hours'),
    ('da000000-0000-0000-0000-000000000005'::uuid, 'DEMO-PN-005', 'NCC-DEMO-005', 'CANCELLED', 4,
     'Phiếu nhập demo đã hủy do nhà cung cấp tạm ngưng.', CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP - INTERVAL '11 days')
) AS receipt(id, code, supplier_code, status, quantity_per_variant, note, created_at, updated_at)
JOIN suppliers supplier ON supplier.code = receipt.supplier_code
ON CONFLICT (id) DO NOTHING;

INSERT INTO goods_receipt_items (
    id, receipt_id, product_variant_id, sku, product_name, cost_price,
    quantity, total, created_at, product_id, target_channel
)
SELECT
    ('dd000001-0000-0000-' || lpad(variant.rn::text, 4, '0') || '-000000000001')::uuid,
    'da000000-0000-0000-0000-000000000001'::uuid,
    variant.variant_id,
    variant.sku,
    variant.product_name,
    100000 + variant.rn * 25000,
    40,
    (100000 + variant.rn * 25000) * 40,
    CURRENT_TIMESTAMP - INTERVAL '20 days',
    variant.product_id,
    'OFFLINE'
FROM warehouse_demo_variants variant
UNION ALL
SELECT
    ('dd000001-0000-0000-' || lpad(variant.rn::text, 4, '0') || '-000000000002')::uuid,
    'da000000-0000-0000-0000-000000000001'::uuid,
    variant.variant_id,
    variant.sku,
    variant.product_name,
    100000 + variant.rn * 25000,
    20,
    (100000 + variant.rn * 25000) * 20,
    CURRENT_TIMESTAMP - INTERVAL '20 days',
    variant.product_id,
    'ONLINE'
FROM warehouse_demo_variants variant
ON CONFLICT (id) DO NOTHING;

WITH receipt_item_templates AS (
    SELECT * FROM (VALUES
        (2, 'da000000-0000-0000-0000-000000000002'::uuid, 12, 'OFFLINE', INTERVAL '2 days'),
        (3, 'da000000-0000-0000-0000-000000000003'::uuid, 8, 'ONLINE', INTERVAL '1 day'),
        (4, 'da000000-0000-0000-0000-000000000004'::uuid, 6, 'OFFLINE', INTERVAL '6 hours'),
        (5, 'da000000-0000-0000-0000-000000000005'::uuid, 4, 'OFFLINE', INTERVAL '12 days')
    ) AS template(document_number, receipt_id, quantity, channel, age)
)
INSERT INTO goods_receipt_items (
    id, receipt_id, product_variant_id, sku, product_name, cost_price,
    quantity, total, created_at, product_id, target_channel
)
SELECT
    ('dd00000' || template.document_number || '-0000-0000-' || lpad(variant.rn::text, 4, '0') || '-000000000001')::uuid,
    template.receipt_id,
    variant.variant_id,
    variant.sku,
    variant.product_name,
    100000 + variant.rn * 25000,
    template.quantity,
    (100000 + variant.rn * 25000) * template.quantity,
    CURRENT_TIMESTAMP - template.age,
    variant.product_id,
    template.channel
FROM receipt_item_templates template
CROSS JOIN warehouse_demo_variants variant
ON CONFLICT (id) DO NOTHING;

INSERT INTO goods_issues (
    id, issue_code, store_id, order_id, issued_by, approved_by, issue_type,
    issue_date, status, note, total_quantity, created_at, updated_at,
    confirmed_at, completed_at, completed_by, supplier_id, reason
)
SELECT
    issue.id,
    issue.code,
    ctx.store_id,
    NULL,
    ctx.actor_id,
    CASE WHEN issue.status IN ('CONFIRMED', 'COMPLETED') THEN ctx.actor_id END,
    issue.issue_type,
    issue.created_at,
    issue.status,
    issue.note,
    (SELECT count(*)::integer * issue.quantity_per_variant FROM warehouse_demo_variants),
    issue.created_at,
    issue.updated_at,
    CASE WHEN issue.status IN ('CONFIRMED', 'COMPLETED') THEN issue.created_at + INTERVAL '1 hour' END,
    CASE WHEN issue.status = 'COMPLETED' THEN issue.created_at + INTERVAL '2 hours' END,
    CASE WHEN issue.status = 'COMPLETED' THEN ctx.actor_id END,
    supplier.id,
    issue.reason
FROM warehouse_demo_context ctx
CROSS JOIN (VALUES
    ('db000000-0000-0000-0000-000000000001'::uuid, 'DEMO-PX-001', 'ONLINE_TO_OFFLINE', 'COMPLETED', 5, NULL::text,
     'Điều chuyển tồn demo từ online sang cửa hàng.', NULL::text, CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '15 days' + INTERVAL '2 hours'),
    ('db000000-0000-0000-0000-000000000002'::uuid, 'DEMO-PX-002', 'DAMAGED', 'COMPLETED', 2, NULL::text,
     'Xuất hàng demo bị hỏng trong quá trình trưng bày.', 'Bao bì rách và sản phẩm bị bẩn.', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days' + INTERVAL '2 hours'),
    ('db000000-0000-0000-0000-000000000003'::uuid, 'DEMO-PX-003', 'RETURN_TO_SUPPLIER', 'COMPLETED', 3, 'NCC-DEMO-001',
     'Trả hàng demo không đạt kiểm tra chất lượng.', 'Sai màu so với mẫu duyệt.', CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP - INTERVAL '7 days' + INTERVAL '2 hours'),
    ('db000000-0000-0000-0000-000000000004'::uuid, 'DEMO-PX-004', 'OTHER', 'CONFIRMED', 4, NULL::text,
     'Phiếu xuất demo đã xác nhận.', 'Xuất mẫu phục vụ buổi chụp hình.', CURRENT_TIMESTAMP - INTERVAL '30 hours', CURRENT_TIMESTAMP - INTERVAL '29 hours'),
    ('db000000-0000-0000-0000-000000000005'::uuid, 'DEMO-PX-005', 'OFFLINE_TO_ONLINE', 'PENDING_CONFIRMATION', 3, NULL::text,
     'Điều chuyển demo từ cửa hàng sang kênh online, chờ xác nhận.', NULL::text, CURRENT_TIMESTAMP - INTERVAL '18 hours', CURRENT_TIMESTAMP - INTERVAL '17 hours'),
    ('db000000-0000-0000-0000-000000000006'::uuid, 'DEMO-PX-006', 'OTHER', 'DRAFT', 1, NULL::text,
     'Phiếu xuất demo đang soạn.', 'Dự kiến xuất cho hoạt động nội bộ.', CURRENT_TIMESTAMP - INTERVAL '4 hours', CURRENT_TIMESTAMP - INTERVAL '3 hours'),
    ('db000000-0000-0000-0000-000000000007'::uuid, 'DEMO-PX-007', 'RETURN_TO_SUPPLIER', 'CANCELLED', 1, 'NCC-DEMO-002',
     'Phiếu trả nhà cung cấp demo đã hủy.', 'Hai bên thống nhất giữ lại hàng.', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '4 days')
) AS issue(id, code, issue_type, status, quantity_per_variant, supplier_code, note, reason, created_at, updated_at)
LEFT JOIN suppliers supplier ON supplier.code = issue.supplier_code
ON CONFLICT (id) DO NOTHING;

WITH issue_item_templates AS (
    SELECT * FROM (VALUES
        (1, 'db000000-0000-0000-0000-000000000001'::uuid, 5, 'ONLINE', INTERVAL '15 days'),
        (2, 'db000000-0000-0000-0000-000000000002'::uuid, 2, 'OFFLINE', INTERVAL '10 days'),
        (3, 'db000000-0000-0000-0000-000000000003'::uuid, 3, 'OFFLINE', INTERVAL '7 days'),
        (4, 'db000000-0000-0000-0000-000000000004'::uuid, 4, 'OFFLINE', INTERVAL '30 hours'),
        (5, 'db000000-0000-0000-0000-000000000005'::uuid, 3, 'OFFLINE', INTERVAL '18 hours'),
        (6, 'db000000-0000-0000-0000-000000000006'::uuid, 1, 'OFFLINE', INTERVAL '4 hours'),
        (7, 'db000000-0000-0000-0000-000000000007'::uuid, 1, 'OFFLINE', INTERVAL '5 days')
    ) AS template(document_number, issue_id, quantity, channel, age)
)
INSERT INTO goods_issue_items (
    id, issue_id, product_variant_id, sku, product_name, quantity,
    created_at, product_id, source_channel
)
SELECT
    ('df00000' || template.document_number || '-0000-0000-' || lpad(variant.rn::text, 4, '0') || '-000000000001')::uuid,
    template.issue_id,
    variant.variant_id,
    variant.sku,
    variant.product_name,
    template.quantity,
    CURRENT_TIMESTAMP - template.age,
    variant.product_id,
    template.channel
FROM issue_item_templates template
CROSS JOIN warehouse_demo_variants variant
ON CONFLICT (id) DO NOTHING;

INSERT INTO inventory_balances (
    store_id, product_variant_id, available_quantity, online_quantity,
    reserved_quantity, damaged_quantity, version, updated_at
)
SELECT
    ctx.store_id,
    variant.variant_id,
    40,
    15,
    0,
    2,
    5,
    CURRENT_TIMESTAMP - INTERVAL '7 days' + INTERVAL '2 hours'
FROM warehouse_demo_context ctx
CROSS JOIN warehouse_demo_variants variant
ON CONFLICT (store_id, product_variant_id) DO NOTHING;

WITH movement_templates AS (
    SELECT * FROM (VALUES
        (1, 'IMPORT', 'GOODS_RECEIPT', 'da000000-0000-0000-0000-000000000001'::uuid,
         40, 40, 0, 40, 0, 0, NULL::varchar, 'OFFLINE'::varchar,
         'da000000-0000-0000-0000-000000000001'::uuid, NULL::uuid, INTERVAL '20 days'),
        (2, 'IMPORT', 'GOODS_RECEIPT', 'da000000-0000-0000-0000-000000000001'::uuid,
         20, 60, 40, 40, 0, 20, NULL::varchar, 'ONLINE'::varchar,
         'da000000-0000-0000-0000-000000000001'::uuid, NULL::uuid, INTERVAL '20 days' - INTERVAL '1 minute'),
        (3, 'ONLINE_TO_OFFLINE', 'GOODS_ISSUE', 'db000000-0000-0000-0000-000000000001'::uuid,
         5, 60, 40, 45, 20, 15, 'ONLINE'::varchar, 'OFFLINE'::varchar,
         NULL::uuid, 'db000000-0000-0000-0000-000000000001'::uuid, INTERVAL '15 days'),
        (4, 'EXPORT_DAMAGED', 'GOODS_ISSUE', 'db000000-0000-0000-0000-000000000002'::uuid,
         -2, 58, 45, 43, 15, 15, 'OFFLINE'::varchar, NULL::varchar,
         NULL::uuid, 'db000000-0000-0000-0000-000000000002'::uuid, INTERVAL '10 days'),
        (5, 'RETURN_TO_SUPPLIER', 'GOODS_ISSUE', 'db000000-0000-0000-0000-000000000003'::uuid,
         -3, 55, 43, 40, 15, 15, 'OFFLINE'::varchar, NULL::varchar,
         NULL::uuid, 'db000000-0000-0000-0000-000000000003'::uuid, INTERVAL '7 days')
    ) AS movement(
        movement_number, transaction_type, reference_type, reference_id,
        quantity, balance_after, before_offline, after_offline, before_online,
        after_online, from_channel, to_channel, import_receipt_id,
        export_receipt_id, age
    )
)
INSERT INTO inventory_transactions (
    id, product_variant_id, store_id, transaction_type, reference_type,
    reference_id, quantity, balance_after, before_offline, after_offline,
    before_online, after_online, from_channel, to_channel, import_receipt_id,
    export_receipt_id, created_by, created_at
)
SELECT
    ('dc00000' || movement.movement_number || '-0000-0000-' || lpad(variant.rn::text, 4, '0') || '-000000000001')::uuid,
    variant.variant_id,
    ctx.store_id,
    movement.transaction_type,
    movement.reference_type,
    movement.reference_id,
    movement.quantity,
    movement.balance_after,
    movement.before_offline,
    movement.after_offline,
    movement.before_online,
    movement.after_online,
    movement.from_channel,
    movement.to_channel,
    movement.import_receipt_id,
    movement.export_receipt_id,
    ctx.actor_id,
    CURRENT_TIMESTAMP - movement.age
FROM warehouse_demo_context ctx
CROSS JOIN warehouse_demo_variants variant
CROSS JOIN movement_templates movement
ON CONFLICT (id) DO NOTHING;

DO $$
BEGIN
    IF (SELECT count(*) FROM suppliers WHERE code LIKE 'NCC-DEMO-%') <> 5 THEN
        RAISE EXCEPTION 'Warehouse demo seed expected exactly five demo suppliers';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM inventory_transactions transaction
        WHERE transaction.id::text LIKE 'dc00000%'
          AND transaction.balance_after <> transaction.after_offline + transaction.after_online
    ) THEN
        RAISE EXCEPTION 'Warehouse demo transaction snapshot is inconsistent';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM warehouse_demo_context ctx
        CROSS JOIN warehouse_demo_variants variant
        LEFT JOIN inventory_balances balance
          ON balance.store_id = ctx.store_id
         AND balance.product_variant_id = variant.variant_id
        WHERE balance.store_id IS NULL
           OR balance.available_quantity < 0
           OR balance.online_quantity < 0
           OR balance.reserved_quantity < 0
           OR balance.damaged_quantity < 0
    ) THEN
        RAISE EXCEPTION 'Warehouse demo balance is missing or negative';
    END IF;
END $$;

COMMIT;

-- Quick post-run summary.
SELECT status, count(*) AS receipt_count
FROM goods_receipts
WHERE receipt_code LIKE 'DEMO-PN-%'
GROUP BY status
ORDER BY status;

SELECT status, issue_type, count(*) AS issue_count
FROM goods_issues
WHERE issue_code LIKE 'DEMO-PX-%'
GROUP BY status, issue_type
ORDER BY status, issue_type;

SELECT count(*) AS supplier_count
FROM suppliers
WHERE code LIKE 'NCC-DEMO-%';
