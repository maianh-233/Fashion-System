-- Development/demo inventory for every active store that has no warehouse data yet.
-- PostgreSQL only. Apply V22, V23 and V30 before running this file.
--
-- Run with:
--   psql -v ON_ERROR_STOP=1 -d <database_name> -f src/main/resources/db/seed/warehouse_store_demo_data.sql
--
-- For each empty store it replays the warehouse workflow in time order, exactly as
-- InventoryService would: completed import receipts add stock, completed export
-- receipts of every issue type move or remove it, and each movement writes a ledger
-- row with before/after snapshots. Draft, pending, confirmed and cancelled receipts
-- are added without touching stock. A few variants end low or out of stock.
-- Stores that already have balances or receipts are skipped, so reruns are no-ops.
-- Timestamps are stored in UTC like the application (hibernate jdbc.time_zone=UTC).

BEGIN;
SET LOCAL lock_timeout = '15s';
SET LOCAL statement_timeout = '5min';
SELECT pg_advisory_xact_lock(hashtext('fashion-system:warehouse-store-demo-data'));
SELECT setseed(0.20261012);

CREATE FUNCTION pg_temp.seed_move(
    p_store uuid, p_variant uuid, p_offline int, p_online int, p_damaged int,
    p_type text, p_ref_type text, p_ref uuid, p_actor uuid, p_from text, p_to text, p_at timestamp
) RETURNS void LANGUAGE plpgsql AS $f$
DECLARE
    v_offline int;
    v_online int;
BEGIN
    INSERT INTO inventory_balances(store_id, product_variant_id, available_quantity, online_quantity,
                                   reserved_quantity, damaged_quantity, version, updated_at)
    VALUES (p_store, p_variant, 0, 0, 0, 0, 0, p_at)
    ON CONFLICT (store_id, product_variant_id) DO NOTHING;

    SELECT available_quantity, online_quantity INTO v_offline, v_online
    FROM inventory_balances WHERE store_id = p_store AND product_variant_id = p_variant FOR UPDATE;

    UPDATE inventory_balances
    SET available_quantity = v_offline + p_offline, online_quantity = v_online + p_online,
        damaged_quantity = damaged_quantity + p_damaged, version = version + 1, updated_at = p_at
    WHERE store_id = p_store AND product_variant_id = p_variant;

    INSERT INTO inventory_transactions(id, product_variant_id, store_id, transaction_type, reference_type,
        reference_id, quantity, balance_after, before_offline, after_offline, before_online, after_online,
        from_channel, to_channel, import_receipt_id, export_receipt_id, created_by, created_at)
    VALUES (gen_random_uuid(), p_variant, p_store, p_type, p_ref_type, p_ref,
        CASE WHEN p_from IS NOT NULL AND p_to IS NOT NULL THEN abs(p_offline) ELSE p_offline + p_online END,
        v_offline + p_offline + v_online + p_online, v_offline, v_offline + p_offline, v_online, v_online + p_online,
        p_from, p_to,
        CASE WHEN p_ref_type = 'GOODS_RECEIPT' THEN p_ref END,
        CASE WHEN p_ref_type = 'GOODS_ISSUE' THEN p_ref END,
        p_actor, p_at);
END
$f$;

CREATE TEMP TABLE seed_pick(
    rn int, variant_id uuid, product_id uuid, sku text, product_name text, price numeric
) ON COMMIT DROP;

DO $$
DECLARE
    v_actor uuid := COALESCE((SELECT id FROM users WHERE username = 'superadmin'),
                             (SELECT id FROM users ORDER BY created_at NULLS LAST, id LIMIT 1));
    v_now timestamp := date_trunc('minute', now() AT TIME ZONE 'UTC');
    v_import_days int[] := ARRAY[60, 50, 41, 33, 25, 17];
    v_export_types text[] := ARRAY['OFFLINE_TO_ONLINE', 'ONLINE_TO_OFFLINE', 'DAMAGED', 'RETURN_TO_SUPPLIER', 'OTHER'];
    v_export_days int[] := ARRAY[14, 11, 8, 6, 4];
    v_open_import_status text[] := ARRAY['DRAFT', 'PENDING_CONFIRMATION', 'CONFIRMED', 'CANCELLED'];
    v_open_import_days int[] := ARRAY[1, 2, 3, 9];
    v_open_export_status text[] := ARRAY['DRAFT', 'PENDING_CONFIRMATION', 'CONFIRMED', 'CANCELLED'];
    v_open_export_types text[] := ARRAY['OFFLINE_TO_ONLINE', 'DAMAGED', 'RETURN_TO_SUPPLIER', 'OTHER'];
    v_open_export_days int[] := ARRAY[1, 2, 3, 7];
    st record;
    it record;
    v_doc uuid;
    v_at timestamp;
    v_done timestamp;
    v_supplier uuid;
    v_type text;
    v_status text;
    v_source text;
    v_reason text;
    v_qty int;
    v_avail int;
    v_cost numeric;
    v_stores int := 0;
BEGIN
    IF v_actor IS NULL THEN RAISE EXCEPTION 'Seed needs at least one user'; END IF;
    IF NOT EXISTS (SELECT 1 FROM suppliers WHERE COALESCE(status, 'ACTIVE') = 'ACTIVE') THEN
        RAISE EXCEPTION 'Seed needs at least one active supplier';
    END IF;

    FOR st IN
        SELECT s.id, s.code FROM stores s
        WHERE COALESCE(s.active, FALSE)
          AND NOT EXISTS (SELECT 1 FROM inventory_balances b WHERE b.store_id = s.id)
          AND NOT EXISTS (SELECT 1 FROM goods_receipts r WHERE r.store_id = s.id)
          AND NOT EXISTS (SELECT 1 FROM goods_issues i WHERE i.store_id = s.id)
        ORDER BY s.code
    LOOP
        v_stores := v_stores + 1;
        DELETE FROM seed_pick;
        INSERT INTO seed_pick
        SELECT row_number() OVER (), x.* FROM (
            SELECT v.id, v.product_id, v.sku, p.name, COALESCE(v.price, 300000)
            FROM product_variants v JOIN products p ON p.id = v.product_id
            WHERE COALESCE(v.active, TRUE) AND COALESCE(p.status, 'ACTIVE') = 'ACTIVE'
            ORDER BY random() LIMIT 48
        ) x;

        -- 1. Completed import receipts: 8 variants each, every picked variant receives stock.
        FOR r IN 1..6 LOOP
            v_doc := gen_random_uuid();
            v_at := v_now - make_interval(days => v_import_days[r], hours => 8 + floor(random() * 6)::int);
            v_done := v_at + interval '3 hours';
            SELECT id INTO v_supplier FROM suppliers WHERE COALESCE(status, 'ACTIVE') = 'ACTIVE' ORDER BY random() LIMIT 1;
            INSERT INTO goods_receipts(id, receipt_code, supplier_id, store_id, received_by, approved_by, receipt_date,
                status, note, total_quantity, total_amount, created_at, updated_at, confirmed_at, completed_at, completed_by)
            VALUES (v_doc, 'IMP-' || to_char(v_at, 'YYYYMMDD') || '-' || replace(gen_random_uuid()::text, '-', ''),
                v_supplier, st.id, v_actor, v_actor, v_at, 'COMPLETED', 'Nhập hàng đợt ' || r, 0, 0,
                v_at, v_done, v_at + interval '1 hour', v_done, v_actor);
            FOR it IN SELECT * FROM seed_pick WHERE rn BETWEEN (r - 1) * 8 + 1 AND r * 8 LOOP
                v_qty := CASE WHEN random() < 0.12 THEN 1 + floor(random() * 4)::int ELSE (2 + floor(random() * 11)::int) * 5 END;
                v_cost := round(it.price * (0.40 + random() * 0.15) / 1000) * 1000;
                v_source := CASE WHEN random() < 0.7 THEN 'OFFLINE' ELSE 'ONLINE' END;
                INSERT INTO goods_receipt_items(id, receipt_id, product_variant_id, sku, product_name, cost_price,
                    quantity, total, created_at, product_id, target_channel)
                VALUES (gen_random_uuid(), v_doc, it.variant_id, it.sku, it.product_name, v_cost,
                    v_qty, v_cost * v_qty, v_at, it.product_id, v_source);
                PERFORM pg_temp.seed_move(st.id, it.variant_id,
                    CASE WHEN v_source = 'OFFLINE' THEN v_qty ELSE 0 END, CASE WHEN v_source = 'ONLINE' THEN v_qty ELSE 0 END, 0,
                    'IMPORT', 'GOODS_RECEIPT', v_doc, v_actor, NULL, v_source, v_done);
            END LOOP;
            UPDATE goods_receipts g SET total_quantity = s.q, total_amount = s.a
            FROM (SELECT sum(quantity) q, sum(total) a FROM goods_receipt_items WHERE receipt_id = v_doc) s WHERE g.id = v_doc;
        END LOOP;

        -- 2. Completed export receipts, one per issue type, applied after all imports.
        FOR r IN 1..5 LOOP
            v_type := v_export_types[r];
            v_source := CASE WHEN v_type = 'ONLINE_TO_OFFLINE' THEN 'ONLINE' ELSE 'OFFLINE' END;
            v_doc := gen_random_uuid();
            v_at := v_now - make_interval(days => v_export_days[r], hours => 8 + floor(random() * 6)::int);
            v_done := v_at + interval '3 hours';
            v_supplier := CASE WHEN v_type = 'RETURN_TO_SUPPLIER'
                THEN (SELECT id FROM suppliers WHERE COALESCE(status, 'ACTIVE') = 'ACTIVE' ORDER BY random() LIMIT 1) END;
            v_reason := CASE v_type
                WHEN 'DAMAGED' THEN 'Hàng lỗi đường may, phai màu'
                WHEN 'RETURN_TO_SUPPLIER' THEN 'Trả hàng lỗi cho nhà cung cấp'
                WHEN 'OTHER' THEN 'Xuất tặng khách hàng thân thiết'
                ELSE 'Cân đối tồn kho giữa kênh online và offline' END;
            INSERT INTO goods_issues(id, issue_code, store_id, issued_by, approved_by, issue_type, issue_date, status,
                note, total_quantity, created_at, updated_at, confirmed_at, completed_at, completed_by, supplier_id, reason)
            VALUES (v_doc, 'EXP-' || to_char(v_at, 'YYYYMMDD') || '-' || replace(gen_random_uuid()::text, '-', ''),
                st.id, v_actor, v_actor, v_type, v_at, 'COMPLETED', NULL, 0, v_at, v_done,
                v_at + interval '1 hour', v_done, v_actor, v_supplier, v_reason);
            -- "OTHER" clears two offline-only variants completely so the store shows out-of-stock SKUs.
            FOR it IN
                SELECT p.*, b.available_quantity, b.online_quantity FROM seed_pick p
                JOIN inventory_balances b ON b.store_id = st.id AND b.product_variant_id = p.variant_id
                WHERE CASE WHEN v_source = 'ONLINE' THEN b.online_quantity ELSE b.available_quantity END >= 5
                ORDER BY CASE WHEN v_type = 'OTHER' AND b.online_quantity = 0 THEN 0 ELSE 1 END, random()
                LIMIT 4
            LOOP
                v_avail := CASE WHEN v_source = 'ONLINE' THEN it.online_quantity ELSE it.available_quantity END;
                v_qty := CASE WHEN v_type = 'OTHER' AND it.online_quantity = 0
                                   AND (SELECT count(*) FROM goods_issue_items WHERE issue_id = v_doc) < 2
                              THEN v_avail
                              ELSE GREATEST(1, floor(v_avail * (0.15 + random() * 0.25))::int) END;
                INSERT INTO goods_issue_items(id, issue_id, product_variant_id, sku, product_name, quantity,
                    created_at, product_id, source_channel)
                VALUES (gen_random_uuid(), v_doc, it.variant_id, it.sku, it.product_name, v_qty, v_at, it.product_id, v_source);
                PERFORM pg_temp.seed_move(st.id, it.variant_id,
                    CASE v_type WHEN 'ONLINE_TO_OFFLINE' THEN v_qty ELSE -v_qty END,
                    CASE v_type WHEN 'ONLINE_TO_OFFLINE' THEN -v_qty WHEN 'OFFLINE_TO_ONLINE' THEN v_qty ELSE 0 END,
                    CASE WHEN v_type = 'DAMAGED' THEN v_qty ELSE 0 END,
                    CASE v_type WHEN 'DAMAGED' THEN 'EXPORT_DAMAGED' WHEN 'OTHER' THEN 'OTHER_EXPORT' ELSE v_type END,
                    'GOODS_ISSUE', v_doc, v_actor, v_source,
                    CASE v_type WHEN 'ONLINE_TO_OFFLINE' THEN 'OFFLINE' WHEN 'OFFLINE_TO_ONLINE' THEN 'ONLINE' END,
                    v_done);
            END LOOP;
            UPDATE goods_issues SET total_quantity = (SELECT COALESCE(sum(quantity), 0) FROM goods_issue_items WHERE issue_id = v_doc)
            WHERE id = v_doc;
        END LOOP;

        -- 3. Import receipts still in the workflow (no stock effect).
        FOR r IN 1..4 LOOP
            v_status := v_open_import_status[r];
            v_doc := gen_random_uuid();
            v_at := v_now - make_interval(days => v_open_import_days[r], hours => 1 + floor(random() * 6)::int);
            SELECT id INTO v_supplier FROM suppliers WHERE COALESCE(status, 'ACTIVE') = 'ACTIVE' ORDER BY random() LIMIT 1;
            INSERT INTO goods_receipts(id, receipt_code, supplier_id, store_id, received_by, approved_by, receipt_date,
                status, note, total_quantity, total_amount, created_at, updated_at, confirmed_at)
            VALUES (v_doc, 'IMP-' || to_char(v_at, 'YYYYMMDD') || '-' || replace(gen_random_uuid()::text, '-', ''),
                v_supplier, st.id, v_actor, CASE WHEN v_status = 'CONFIRMED' THEN v_actor END, v_at, v_status,
                CASE v_status WHEN 'CANCELLED' THEN 'Hủy do nhà cung cấp giao sai mẫu' ELSE 'Bổ sung hàng bán chạy' END,
                0, 0, v_at, v_at + interval '30 minutes', CASE WHEN v_status = 'CONFIRMED' THEN v_at + interval '1 hour' END);
            FOR it IN SELECT * FROM seed_pick ORDER BY random() LIMIT 3 + floor(random() * 3)::int LOOP
                v_qty := (2 + floor(random() * 9)::int) * 5;
                v_cost := round(it.price * (0.40 + random() * 0.15) / 1000) * 1000;
                INSERT INTO goods_receipt_items(id, receipt_id, product_variant_id, sku, product_name, cost_price,
                    quantity, total, created_at, product_id, target_channel)
                VALUES (gen_random_uuid(), v_doc, it.variant_id, it.sku, it.product_name, v_cost, v_qty, v_cost * v_qty,
                    v_at, it.product_id, CASE WHEN random() < 0.7 THEN 'OFFLINE' ELSE 'ONLINE' END);
            END LOOP;
            UPDATE goods_receipts g SET total_quantity = s.q, total_amount = s.a
            FROM (SELECT sum(quantity) q, sum(total) a FROM goods_receipt_items WHERE receipt_id = v_doc) s WHERE g.id = v_doc;
        END LOOP;

        -- 4. Export receipts still in the workflow; quantities stay within current offline stock.
        FOR r IN 1..4 LOOP
            v_status := v_open_export_status[r];
            v_type := v_open_export_types[r];
            v_doc := gen_random_uuid();
            v_at := v_now - make_interval(days => v_open_export_days[r], hours => 1 + floor(random() * 6)::int);
            INSERT INTO goods_issues(id, issue_code, store_id, issued_by, approved_by, issue_type, issue_date, status,
                note, total_quantity, created_at, updated_at, confirmed_at, supplier_id, reason)
            VALUES (v_doc, 'EXP-' || to_char(v_at, 'YYYYMMDD') || '-' || replace(gen_random_uuid()::text, '-', ''),
                st.id, v_actor, CASE WHEN v_status = 'CONFIRMED' THEN v_actor END, v_type, v_at, v_status, NULL, 0,
                v_at, v_at + interval '30 minutes', CASE WHEN v_status = 'CONFIRMED' THEN v_at + interval '1 hour' END,
                CASE WHEN v_type = 'RETURN_TO_SUPPLIER'
                     THEN (SELECT id FROM suppliers WHERE COALESCE(status, 'ACTIVE') = 'ACTIVE' ORDER BY random() LIMIT 1) END,
                CASE v_type
                    WHEN 'DAMAGED' THEN 'Hàng trưng bày bị bẩn, rách'
                    WHEN 'RETURN_TO_SUPPLIER' THEN 'Trả lại hàng sai size cho nhà cung cấp'
                    WHEN 'OTHER' THEN 'Xuất mẫu cho buổi chụp hình'
                    ELSE 'Chuyển hàng lên kênh online' END);
            FOR it IN
                SELECT p.*, b.available_quantity FROM seed_pick p
                JOIN inventory_balances b ON b.store_id = st.id AND b.product_variant_id = p.variant_id
                WHERE b.available_quantity >= 6 ORDER BY random() LIMIT 2 + floor(random() * 2)::int
            LOOP
                INSERT INTO goods_issue_items(id, issue_id, product_variant_id, sku, product_name, quantity,
                    created_at, product_id, source_channel)
                VALUES (gen_random_uuid(), v_doc, it.variant_id, it.sku, it.product_name,
                    GREATEST(1, floor(it.available_quantity * (0.1 + random() * 0.2))::int), v_at, it.product_id, 'OFFLINE');
            END LOOP;
            UPDATE goods_issues SET total_quantity = (SELECT COALESCE(sum(quantity), 0) FROM goods_issue_items WHERE issue_id = v_doc)
            WHERE id = v_doc;
        END LOOP;
    END LOOP;

    RAISE NOTICE 'Seeded warehouse demo data for % store(s)', v_stores;
END
$$;

COMMIT;
