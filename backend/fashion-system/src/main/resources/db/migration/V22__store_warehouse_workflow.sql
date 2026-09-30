-- Explicit psql migration; this project does not automatically run Flyway.
-- Stop application writers and take a backup before applying (see docs).
BEGIN;
SET LOCAL lock_timeout = '15s';
SET LOCAL statement_timeout = '5min';
LOCK TABLE inventory_balances, inventory_transactions, goods_receipts,
    goods_receipt_items, goods_issues, goods_issue_items IN ACCESS EXCLUSIVE MODE;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM goods_receipts WHERE status NOT IN ('PENDING','APPROVED','DRAFT','PENDING_CONFIRMATION','CONFIRMED','COMPLETED','CANCELLED'))
       OR EXISTS (SELECT 1 FROM goods_issues WHERE status NOT IN ('PENDING','APPROVED','DRAFT','PENDING_CONFIRMATION','CONFIRMED','COMPLETED','CANCELLED')) THEN
        RAISE EXCEPTION 'V22: unsupported legacy receipt status; inspect and explicitly reconcile before retrying';
    END IF;
    IF EXISTS (SELECT 1 FROM inventory_balances WHERE available_quantity < 0 OR reserved_quantity < 0 OR damaged_quantity < 0) THEN
        RAISE EXCEPTION 'V22: negative legacy inventory; reconcile without deleting stock history before retrying';
    END IF;
END $$;

ALTER TABLE inventory_balances
    ADD COLUMN IF NOT EXISTS online_quantity INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
COMMENT ON COLUMN inventory_balances.available_quantity IS 'Offline stock; retained legacy column name. Total stock = available_quantity + online_quantity.';

ALTER TABLE goods_receipts
    ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS completed_by UUID;
ALTER TABLE goods_issues
    ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS completed_by UUID,
    ADD COLUMN IF NOT EXISTS supplier_id UUID,
    ADD COLUMN IF NOT EXISTS reason TEXT;

-- APPROVED already moved stock. Do not replay movements or manufacture timestamps
-- where the original record has none. Creator/approver identifiers are retained.
UPDATE goods_receipts SET status = 'COMPLETED', completed_by = approved_by,
    confirmed_at = updated_at, completed_at = updated_at WHERE status = 'APPROVED';
UPDATE goods_issues SET status = 'COMPLETED', completed_by = approved_by,
    confirmed_at = updated_at, completed_at = updated_at WHERE status = 'APPROVED';
UPDATE goods_receipts SET status = 'DRAFT' WHERE status = 'PENDING';
UPDATE goods_issues SET status = 'DRAFT' WHERE status = 'PENDING';
UPDATE goods_issues SET reason = concat_ws(E'\n', NULLIF(reason, ''), 'Legacy issue type: ' || issue_type),
    issue_type = 'OTHER'
WHERE issue_type NOT IN ('RETURN_TO_SUPPLIER','DAMAGED','OTHER','ONLINE_TO_OFFLINE','OFFLINE_TO_ONLINE');

ALTER TABLE goods_receipt_items
    ADD COLUMN IF NOT EXISTS product_id UUID,
    ADD COLUMN IF NOT EXISTS target_channel VARCHAR(20) NOT NULL DEFAULT 'OFFLINE';
ALTER TABLE goods_issue_items
    ADD COLUMN IF NOT EXISTS product_id UUID,
    ADD COLUMN IF NOT EXISTS source_channel VARCHAR(20) NOT NULL DEFAULT 'OFFLINE';
UPDATE goods_receipt_items i SET product_id = v.product_id FROM product_variants v
    WHERE i.product_variant_id = v.id AND i.product_id IS NULL;
UPDATE goods_issue_items i SET product_id = v.product_id FROM product_variants v
    WHERE i.product_variant_id = v.id AND i.product_id IS NULL;
-- An orphan variant aborts the transaction instead of discarding historical items.
ALTER TABLE goods_receipt_items ALTER COLUMN product_id SET NOT NULL;
ALTER TABLE goods_issue_items ALTER COLUMN product_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_warehouse_variant_product ON product_variants(id, product_id);

ALTER TABLE inventory_transactions
    ADD COLUMN IF NOT EXISTS before_offline INTEGER,
    ADD COLUMN IF NOT EXISTS after_offline INTEGER,
    ADD COLUMN IF NOT EXISTS before_online INTEGER,
    ADD COLUMN IF NOT EXISTS after_online INTEGER,
    ADD COLUMN IF NOT EXISTS from_channel VARCHAR(20),
    ADD COLUMN IF NOT EXISTS to_channel VARCHAR(20),
    ADD COLUMN IF NOT EXISTS import_receipt_id UUID,
    ADD COLUMN IF NOT EXISTS export_receipt_id UUID;
-- Legacy ledger snapshots and typed references remain NULL: historic online state
-- and generic reference_id provenance cannot be inferred reliably.

-- Recreate state checks so rerunning also upgrades an earlier V22 draft that
-- omitted the submitted state. No stock or historical rows are changed.
ALTER TABLE goods_receipts DROP CONSTRAINT IF EXISTS ck_warehouse_receipt_status;
ALTER TABLE goods_issues DROP CONSTRAINT IF EXISTS ck_warehouse_issue_status;
-- Install named constraints only once so a verified rerun is harmless.
DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN SELECT * FROM (VALUES
      ('inventory_balances','ck_warehouse_balances_nonnegative', 'CHECK (available_quantity >= 0 AND online_quantity >= 0 AND reserved_quantity >= 0 AND damaged_quantity >= 0 AND version >= 0 AND available_quantity::bigint + online_quantity::bigint <= 2147483647)'),
      ('inventory_balances','fk_warehouse_balance_variant', 'FOREIGN KEY (product_variant_id) REFERENCES product_variants(id)'),
      ('goods_receipts','ck_warehouse_receipt_status', 'CHECK (status IN (''DRAFT'',''PENDING_CONFIRMATION'',''CONFIRMED'',''COMPLETED'',''CANCELLED''))'),
      ('goods_receipts','ck_warehouse_receipt_supplier', 'CHECK (status IN (''DRAFT'',''CANCELLED'') OR supplier_id IS NOT NULL) NOT VALID'),
      ('goods_receipts','fk_warehouse_receipt_completed_by', 'FOREIGN KEY (completed_by) REFERENCES users(id)'),
      ('goods_issues','ck_warehouse_issue_status', 'CHECK (status IN (''DRAFT'',''PENDING_CONFIRMATION'',''CONFIRMED'',''COMPLETED'',''CANCELLED''))'),
      ('goods_issues','ck_warehouse_issue_supplier', 'CHECK (status IN (''DRAFT'',''CANCELLED'') OR issue_type <> ''RETURN_TO_SUPPLIER'' OR supplier_id IS NOT NULL) NOT VALID'),
      ('goods_issues','ck_warehouse_issue_reason', 'CHECK (status IN (''DRAFT'',''CANCELLED'') OR issue_type <> ''OTHER'' OR NULLIF(btrim(reason), '''') IS NOT NULL) NOT VALID'),
      ('goods_issues','ck_warehouse_issue_type', 'CHECK (issue_type IN (''RETURN_TO_SUPPLIER'',''DAMAGED'',''OTHER'',''ONLINE_TO_OFFLINE'',''OFFLINE_TO_ONLINE''))'),
      ('goods_issues','fk_warehouse_issue_supplier', 'FOREIGN KEY (supplier_id) REFERENCES suppliers(id)'),
      ('goods_issues','fk_warehouse_issue_completed_by', 'FOREIGN KEY (completed_by) REFERENCES users(id)'),
      ('goods_receipt_items','ck_warehouse_receipt_item_channel', 'CHECK (target_channel IN (''ONLINE'',''OFFLINE''))'),
      ('goods_issue_items','ck_warehouse_issue_item_channel', 'CHECK (source_channel IN (''ONLINE'',''OFFLINE''))'),
      ('goods_receipt_items','ck_warehouse_receipt_item_quantity', 'CHECK (quantity > 0) NOT VALID'),
      ('goods_receipt_items','ck_warehouse_receipt_item_cost', 'CHECK (cost_price >= 0) NOT VALID'),
      ('goods_issue_items','ck_warehouse_issue_item_quantity', 'CHECK (quantity > 0) NOT VALID'),
      ('goods_receipt_items','fk_warehouse_receipt_item_product', 'FOREIGN KEY (product_id) REFERENCES products(id)'),
      ('goods_issue_items','fk_warehouse_issue_item_product', 'FOREIGN KEY (product_id) REFERENCES products(id)'),
      ('goods_receipt_items','fk_warehouse_receipt_item_variant_product', 'FOREIGN KEY (product_variant_id, product_id) REFERENCES product_variants(id, product_id)'),
      ('goods_issue_items','fk_warehouse_issue_item_variant_product', 'FOREIGN KEY (product_variant_id, product_id) REFERENCES product_variants(id, product_id)'),
      ('inventory_transactions','fk_warehouse_transaction_variant', 'FOREIGN KEY (product_variant_id) REFERENCES product_variants(id)'),
      ('inventory_transactions','fk_warehouse_transaction_import', 'FOREIGN KEY (import_receipt_id) REFERENCES goods_receipts(id)'),
      ('inventory_transactions','fk_warehouse_transaction_export', 'FOREIGN KEY (export_receipt_id) REFERENCES goods_issues(id)'),
      ('inventory_transactions','ck_warehouse_transaction_channels', 'CHECK ((from_channel IS NULL OR from_channel IN (''ONLINE'',''OFFLINE'')) AND (to_channel IS NULL OR to_channel IN (''ONLINE'',''OFFLINE'')))'),
      ('inventory_transactions','ck_warehouse_transaction_snapshots', 'CHECK ((before_offline IS NULL AND after_offline IS NULL AND before_online IS NULL AND after_online IS NULL) OR (before_offline IS NOT NULL AND after_offline IS NOT NULL AND before_online IS NOT NULL AND after_online IS NOT NULL AND before_offline >= 0 AND after_offline >= 0 AND before_online >= 0 AND after_online >= 0 AND before_offline::bigint + before_online::bigint <= 2147483647 AND after_offline::bigint + after_online::bigint <= 2147483647))'),
      ('inventory_transactions','ck_warehouse_transaction_reference', 'CHECK (import_receipt_id IS NULL OR export_receipt_id IS NULL)')
    ) AS constraints_to_add(table_name, constraint_name, definition)
    LOOP
      IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = to_regclass(c.table_name) AND conname = c.constraint_name) THEN
        EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I %s', c.table_name, c.constraint_name, c.definition);
      END IF;
    END LOOP;
END $$;

CREATE INDEX IF NOT EXISTS idx_warehouse_balance_variant ON inventory_balances(product_variant_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_transaction_variant ON inventory_transactions(product_variant_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_transaction_import ON inventory_transactions(import_receipt_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_transaction_export ON inventory_transactions(export_receipt_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_transaction_store_created ON inventory_transactions(store_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_warehouse_receipt_completed_by ON goods_receipts(completed_by);
CREATE INDEX IF NOT EXISTS idx_warehouse_issue_completed_by ON goods_issues(completed_by);
CREATE INDEX IF NOT EXISTS idx_warehouse_issue_supplier ON goods_issues(supplier_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_receipt_item_product ON goods_receipt_items(product_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_issue_item_product ON goods_issue_items(product_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_receipt_store_status ON goods_receipts(store_id, status);
CREATE INDEX IF NOT EXISTS idx_warehouse_issue_store_status ON goods_issues(store_id, status);

-- Require the existing authorization catalog rather than silently omit new rights.
DO $$ BEGIN
    IF (SELECT count(*) FROM permission_groups WHERE code IN ('IMPORT_RECEIPT','EXPORT_RECEIPT')) <> 2 THEN
        RAISE EXCEPTION 'V22: seed IMPORT_RECEIPT and EXPORT_RECEIPT permission groups before migrating';
    END IF;
END $$;
INSERT INTO permissions (code, name, group_id, description)
SELECT g.code || '_' || a.action, a.label || ' ' || g.name, g.id,
       'Warehouse workflow: ' || lower(a.action)
FROM permission_groups g CROSS JOIN (VALUES ('CONFIRM','Confirm'),('COMPLETE','Complete'),('CANCEL','Cancel')) a(action,label)
WHERE g.code IN ('IMPORT_RECEIPT','EXPORT_RECEIPT') ON CONFLICT (code) DO NOTHING;

-- Preserve scope and direct ALLOW/DENY overrides. Existing target grants win.
WITH mappings(old_suffix,new_suffix) AS (VALUES ('APPROVE','CONFIRM'),('DELETE','CANCEL'))
INSERT INTO role_permissions (role_id, permission_id, scope)
SELECT rp.role_id, np.id, rp.scope FROM role_permissions rp
JOIN permissions op ON op.id = rp.permission_id
JOIN mappings m ON op.code IN ('IMPORT_RECEIPT_' || m.old_suffix, 'EXPORT_RECEIPT_' || m.old_suffix)
JOIN permissions np ON np.code = regexp_replace(op.code, m.old_suffix || '$', m.new_suffix)
ON CONFLICT (role_id, permission_id) DO NOTHING;
WITH mappings(old_suffix,new_suffix) AS (VALUES ('APPROVE','CONFIRM'),('DELETE','CANCEL'))
INSERT INTO user_permissions (user_id, permission_id, effect, scope)
SELECT up.user_id, np.id, up.effect, up.scope FROM user_permissions up
JOIN permissions op ON op.id = up.permission_id
JOIN mappings m ON op.code IN ('IMPORT_RECEIPT_' || m.old_suffix, 'EXPORT_RECEIPT_' || m.old_suffix)
JOIN permissions np ON np.code = regexp_replace(op.code, m.old_suffix || '$', m.new_suffix)
ON CONFLICT (user_id, permission_id) DO NOTHING;
-- COMPLETE changes stock and is deliberately not inherited from APPROVE.
-- Only the existing ADMIN role receives it; administrators assign other grants.
INSERT INTO role_permissions (role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code IN ('IMPORT_RECEIPT_COMPLETE','EXPORT_RECEIPT_COMPLETE')
ON CONFLICT (role_id, permission_id) DO NOTHING;
COMMIT;
