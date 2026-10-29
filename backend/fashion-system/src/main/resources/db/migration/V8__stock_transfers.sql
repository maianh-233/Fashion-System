-- Điều chuyển hàng giữa cửa hàng (tồn tại quầy): cửa hàng thiếu hàng tạo yêu cầu (REQUESTED) → cửa hàng gửi
-- duyệt và xuất (SHIPPED: trừ tồn nơi gửi, hàng đang đi đường) hoặc từ chối (REJECTED) → cửa hàng nhận xác nhận
-- (RECEIVED: cộng tồn nơi nhận). Bên yêu cầu hủy được khi chưa duyệt (CANCELLED).

CREATE SEQUENCE stock_transfer_code_seq;

CREATE TABLE stock_transfers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transfer_code   VARCHAR(30) NOT NULL UNIQUE,
    from_store_id   UUID NOT NULL REFERENCES stores(id),
    to_store_id     UUID NOT NULL REFERENCES stores(id),
    status          VARCHAR(20) NOT NULL CHECK (status IN ('REQUESTED', 'SHIPPED', 'RECEIVED', 'REJECTED', 'CANCELLED')),
    note            VARCHAR(1000),
    reject_reason   VARCHAR(500),
    requested_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    shipped_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    received_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    shipped_at      TIMESTAMP,
    received_at     TIMESTAMP,
    updated_at      TIMESTAMP,
    CONSTRAINT ck_stock_transfers_stores CHECK (from_store_id <> to_store_id)
);

CREATE INDEX idx_stock_transfers_from ON stock_transfers (from_store_id, status, created_at DESC);
CREATE INDEX idx_stock_transfers_to ON stock_transfers (to_store_id, status, created_at DESC);

CREATE TABLE stock_transfer_items (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transfer_id        UUID NOT NULL REFERENCES stock_transfers(id) ON DELETE CASCADE,
    product_variant_id UUID NOT NULL REFERENCES product_variants(id),
    requested_quantity INTEGER NOT NULL CHECK (requested_quantity > 0),
    shipped_quantity   INTEGER CHECK (shipped_quantity >= 0),
    CONSTRAINT uq_stock_transfer_items_variant UNIQUE (transfer_id, product_variant_id)
);

-- "Điều chuyển hàng" permission group (own sidebar entry) in the WAREHOUSE module.
INSERT INTO permission_groups(id, name, code, description, created_at, module_id)
SELECT (substr(md5('permission_group:TRANSFER'),1,8) || '-' || substr(md5('permission_group:TRANSFER'),9,4) || '-' ||
        substr(md5('permission_group:TRANSFER'),13,4) || '-' || substr(md5('permission_group:TRANSFER'),17,4) || '-' ||
        substr(md5('permission_group:TRANSFER'),21,12))::uuid,
       'Điều chuyển hàng', 'TRANSFER', 'Yêu cầu, xuất và nhận hàng chuyển giữa các cửa hàng', current_timestamp, m.id
FROM modules m WHERE m.code = 'WAREHOUSE'
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions(id, code, name, group_id, description, created_at)
SELECT (substr(md5('permission:' || p.code),1,8) || '-' || substr(md5('permission:' || p.code),9,4) || '-' ||
        substr(md5('permission:' || p.code),13,4) || '-' || substr(md5('permission:' || p.code),17,4) || '-' ||
        substr(md5('permission:' || p.code),21,12))::uuid,
       p.code, p.name, g.id, p.description, current_timestamp
FROM permission_groups g
CROSS JOIN (VALUES
    ('TRANSFER_VIEW', 'Xem điều chuyển', 'Xem phiếu điều chuyển và hàng sắp hết của cửa hàng'),
    ('TRANSFER_MANAGE', 'Xử lý điều chuyển', 'Tạo yêu cầu, duyệt / xuất, từ chối và xác nhận nhận hàng điều chuyển')
) AS p(code, name, description)
WHERE g.code = 'TRANSFER'
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r CROSS JOIN permissions p
WHERE (r.code IN ('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'WAREHOUSE') AND p.code IN ('TRANSFER_VIEW', 'TRANSFER_MANAGE'))
   OR (r.code = 'STAFF' AND p.code = 'TRANSFER_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;
