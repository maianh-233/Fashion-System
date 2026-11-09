-- Các trường hợp đặc biệt của thanh toán, bán tại quầy và nhập / xuất hàng.

-- Kế toán xử lý yêu cầu hoàn tiền phát sinh từ thanh toán (tiền về sau khi hủy, trả trùng, hủy sau giao thất bại).
ALTER TABLE refunds ADD COLUMN duplicate_payment BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE refunds ADD COLUMN reject_reason VARCHAR(500);

INSERT INTO permission_groups(id, name, code, description, created_at, module_id)
SELECT (substr(md5('permission_group:REFUND'),1,8) || '-' || substr(md5('permission_group:REFUND'),9,4) || '-' ||
        substr(md5('permission_group:REFUND'),13,4) || '-' || substr(md5('permission_group:REFUND'),17,4) || '-' ||
        substr(md5('permission_group:REFUND'),21,12))::uuid,
       'Hoàn tiền', 'REFUND', 'Xử lý các khoản hoàn tiền phát sinh từ thanh toán online', current_timestamp, m.id
FROM modules m WHERE m.code = 'SALES'
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions(id, code, name, group_id, description, created_at)
SELECT (substr(md5('permission:REFUND_MANAGE'),1,8) || '-' || substr(md5('permission:REFUND_MANAGE'),9,4) || '-' ||
        substr(md5('permission:REFUND_MANAGE'),13,4) || '-' || substr(md5('permission:REFUND_MANAGE'),17,4) || '-' ||
        substr(md5('permission:REFUND_MANAGE'),21,12))::uuid,
       'REFUND_MANAGE', 'Xử lý hoàn tiền', g.id, 'Xem và xác nhận đã hoàn tiền hoặc từ chối yêu cầu hoàn tiền', current_timestamp
FROM permission_groups g WHERE g.code = 'REFUND'
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN', 'ACCOUNTANT') AND p.code = 'REFUND_MANAGE'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Gửi lại cùng một lần thanh toán tại quầy không tạo thêm đơn (khách vãng lai không có customer_id nên cần chỉ mục riêng).
CREATE UNIQUE INDEX uq_orders_pos_request ON orders(store_id, client_request_id)
    WHERE order_type = 'OFFLINE' AND client_request_id IS NOT NULL;

-- Hủy phiếu đã duyệt phải có lý do; phiếu nhập ghi được số thực nhận (dòng không về có quantity = 0).
ALTER TABLE goods_receipts ADD COLUMN cancel_reason VARCHAR(500);
ALTER TABLE goods_issues ADD COLUMN cancel_reason VARCHAR(500);
ALTER TABLE goods_receipt_items ADD COLUMN ordered_quantity INT;
ALTER TABLE goods_receipt_items DROP CONSTRAINT ck_warehouse_receipt_item_quantity;
ALTER TABLE goods_receipt_items ADD CONSTRAINT ck_warehouse_receipt_item_quantity CHECK (quantity >= 0) NOT VALID;
