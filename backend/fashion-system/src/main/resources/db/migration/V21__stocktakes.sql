-- Kiểm kê kho: phiếu kiểm kê theo cửa hàng (toàn bộ hoặc một danh mục kể cả danh mục con) → nhân viên quét mã vạch / SKU
-- để đếm → gửi duyệt → quản lý duyệt thì sinh phiếu điều chỉnh tồn kho (ghi sổ STOCKTAKE_ADJUST) hoặc trả về đếm lại.
-- Số tồn hệ thống được chụp lúc tạo phiếu; khi duyệt chỉ cộng / trừ phần chênh lệch vào tồn hiện tại nên bán hàng trong
-- lúc đếm vẫn đúng. Tồn kiểm = tại quầy + online + đang giữ cho đơn (không gồm hàng hỏng).

CREATE SEQUENCE stocktake_code_seq;
CREATE SEQUENCE stock_adjustment_code_seq;

CREATE TABLE stocktakes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    store_id UUID NOT NULL REFERENCES stores(id),
    scope_type VARCHAR(20) NOT NULL CHECK (scope_type IN ('STORE', 'CATEGORY')),
    category_id UUID REFERENCES categories(id),
    blind_count BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'APPROVED', 'CANCELLED')),
    note VARCHAR(1000),
    zero_uncounted BOOLEAN NOT NULL DEFAULT FALSE,
    rejection_reason VARCHAR(1000),
    approver_note VARCHAR(1000),
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    submitted_by UUID REFERENCES users(id) ON DELETE SET NULL,
    decided_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    submitted_at TIMESTAMP,
    decided_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT chk_stocktakes_scope CHECK ((scope_type = 'CATEGORY') = (category_id IS NOT NULL))
);
-- Mỗi cửa hàng chỉ một phiếu đang mở để số đếm không chồng lên nhau.
CREATE UNIQUE INDEX uq_stocktakes_open_store ON stocktakes(store_id) WHERE status IN ('IN_PROGRESS', 'SUBMITTED');
CREATE INDEX idx_stocktakes_store_status ON stocktakes(store_id, status, created_at DESC);
CREATE INDEX idx_stocktakes_decided ON stocktakes(status, decided_at);

CREATE TABLE stocktake_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stocktake_id UUID NOT NULL REFERENCES stocktakes(id) ON DELETE CASCADE,
    product_variant_id UUID NOT NULL REFERENCES product_variants(id),
    system_quantity INT NOT NULL CHECK (system_quantity >= 0),
    counted_quantity INT CHECK (counted_quantity >= 0),
    unit_cost NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (unit_cost >= 0),
    cost_source VARCHAR(20) NOT NULL DEFAULT 'RECEIPT' CHECK (cost_source IN ('RECEIPT', 'PRICE', 'NONE')),
    added_during_count BOOLEAN NOT NULL DEFAULT FALSE,
    note VARCHAR(500),
    counted_by UUID REFERENCES users(id) ON DELETE SET NULL,
    counted_at TIMESTAMP,
    -- Số thực cộng / trừ vào tồn khi duyệt (có thể khác chênh lệch nếu tồn hiện tại không đủ để trừ).
    adjusted_quantity INT,
    CONSTRAINT uq_stocktake_items_variant UNIQUE (stocktake_id, product_variant_id)
);
CREATE INDEX idx_stocktake_items_stocktake ON stocktake_items(stocktake_id);

-- Nhật ký từng lần quét / nhập số (nhiều người đếm cùng lúc).
CREATE TABLE stocktake_count_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID NOT NULL REFERENCES stocktake_items(id) ON DELETE CASCADE,
    mode VARCHAR(10) NOT NULL CHECK (mode IN ('SCAN', 'SET')),
    quantity INT NOT NULL,
    counted_after INT NOT NULL,
    scanned_code VARCHAR(100),
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);
CREATE INDEX idx_stocktake_count_logs_item ON stocktake_count_logs(item_id, created_at DESC);

-- Phiếu điều chỉnh tồn kho sinh ra khi duyệt kiểm kê.
CREATE TABLE stock_adjustments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    stocktake_id UUID NOT NULL UNIQUE REFERENCES stocktakes(id),
    store_id UUID NOT NULL REFERENCES stores(id),
    increase_quantity INT NOT NULL DEFAULT 0,
    decrease_quantity INT NOT NULL DEFAULT 0,
    increase_value NUMERIC(16,2) NOT NULL DEFAULT 0,
    decrease_value NUMERIC(16,2) NOT NULL DEFAULT 0,
    line_count INT NOT NULL DEFAULT 0,
    approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);
CREATE INDEX idx_stock_adjustments_store ON stock_adjustments(store_id, created_at DESC);

-- ========== QUYỀN ==========

INSERT INTO permission_groups(id, name, code, description, created_at, module_id)
SELECT (substr(md5('permission_group:STOCKTAKE'),1,8) || '-' || substr(md5('permission_group:STOCKTAKE'),9,4) || '-' ||
        substr(md5('permission_group:STOCKTAKE'),13,4) || '-' || substr(md5('permission_group:STOCKTAKE'),17,4) || '-' ||
        substr(md5('permission_group:STOCKTAKE'),21,12))::uuid,
       'Kiểm kê kho', 'STOCKTAKE', 'Phiếu kiểm kê, đếm hàng bằng mã vạch, duyệt điều chỉnh tồn và báo cáo hao hụt', current_timestamp, m.id
FROM modules m WHERE m.code = 'WAREHOUSE'
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions(id, code, name, group_id, description, created_at)
SELECT (substr(md5('permission:' || p.code),1,8) || '-' || substr(md5('permission:' || p.code),9,4) || '-' ||
        substr(md5('permission:' || p.code),13,4) || '-' || substr(md5('permission:' || p.code),17,4) || '-' ||
        substr(md5('permission:' || p.code),21,12))::uuid,
       p.code, p.name, g.id, p.description, current_timestamp
FROM permission_groups g
CROSS JOIN (VALUES
    ('STOCKTAKE_VIEW', 'Xem kiểm kê', 'Xem phiếu kiểm kê và báo cáo hao hụt của cửa hàng'),
    ('STOCKTAKE_COUNT', 'Đếm kiểm kê', 'Tạo phiếu kiểm kê, quét mã vạch / nhập số đếm và gửi duyệt'),
    ('STOCKTAKE_APPROVE', 'Duyệt kiểm kê', 'Duyệt kiểm kê (sinh phiếu điều chỉnh tồn), trả về đếm lại hoặc hủy phiếu')
) AS p(code, name, description)
WHERE g.code = 'STOCKTAKE'
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r CROSS JOIN permissions p
WHERE (r.code IN ('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'WAREHOUSE') AND p.code IN ('STOCKTAKE_VIEW', 'STOCKTAKE_COUNT', 'STOCKTAKE_APPROVE'))
   OR (r.code = 'STAFF' AND p.code IN ('STOCKTAKE_VIEW', 'STOCKTAKE_COUNT'))
   OR (r.code = 'ACCOUNTANT' AND p.code = 'STOCKTAKE_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;
