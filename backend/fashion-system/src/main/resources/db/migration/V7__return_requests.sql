-- Đổi / trả hàng: khách gửi yêu cầu cho đơn đã hoàn thành (trong 14 ngày), cửa hàng duyệt, khách mang hàng
-- tới cửa hàng đã bán, nhân viên kiểm tra từng món (còn tốt → tồn tại quầy, lỗi → tồn hỏng) rồi hoàn tất:
-- trả hàng thì hoàn tiền (tiền mặt/COD tại quầy, VNPay/MoMo tạo yêu cầu hoàn cho kế toán), đổi hàng thì
-- xuất phân loại mới từ tồn tại quầy.

CREATE SEQUENCE return_request_code_seq;

CREATE TABLE return_requests (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_code   VARCHAR(30) NOT NULL UNIQUE,
    order_id       UUID NOT NULL REFERENCES orders(id),
    customer_id    UUID NOT NULL REFERENCES customers(id),
    store_id       UUID NOT NULL REFERENCES stores(id),
    request_type   VARCHAR(20) NOT NULL CHECK (request_type IN ('RETURN', 'EXCHANGE')),
    status         VARCHAR(20) NOT NULL CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED')),
    reason_code    VARCHAR(30) NOT NULL,
    note           VARCHAR(1000),
    reject_reason  VARCHAR(500),
    refund_amount  NUMERIC(14, 2),
    refund_id      UUID REFERENCES refunds(id),
    handled_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    approved_at    TIMESTAMP,
    completed_at   TIMESTAMP,
    updated_at     TIMESTAMP
);

CREATE INDEX idx_return_requests_order ON return_requests (order_id);
CREATE INDEX idx_return_requests_customer ON return_requests (customer_id, created_at DESC);
CREATE INDEX idx_return_requests_store_status ON return_requests (store_id, status, created_at DESC);

CREATE TABLE return_request_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id          UUID NOT NULL REFERENCES return_requests(id) ON DELETE CASCADE,
    order_item_id       UUID NOT NULL REFERENCES order_items(id),
    product_variant_id  UUID REFERENCES product_variants(id) ON DELETE SET NULL,
    quantity            INTEGER NOT NULL CHECK (quantity > 0),
    unit_price          NUMERIC(14, 2) NOT NULL CHECK (unit_price >= 0),
    exchange_variant_id UUID REFERENCES product_variants(id),
    item_condition      VARCHAR(20) CHECK (item_condition IN ('GOOD', 'DAMAGED')),
    CONSTRAINT uq_return_request_items_line UNIQUE (request_id, order_item_id)
);

CREATE INDEX idx_return_request_items_order_item ON return_request_items (order_item_id);

-- "Đổi trả" permission group (own sidebar entry) in the SALES module; store scope comes from store staff.
INSERT INTO permission_groups(id, name, code, description, created_at, module_id)
SELECT (substr(md5('permission_group:RETURN'),1,8) || '-' || substr(md5('permission_group:RETURN'),9,4) || '-' ||
        substr(md5('permission_group:RETURN'),13,4) || '-' || substr(md5('permission_group:RETURN'),17,4) || '-' ||
        substr(md5('permission_group:RETURN'),21,12))::uuid,
       'Đổi trả', 'RETURN', 'Duyệt và nhận hàng đổi / trả của khách', current_timestamp, m.id
FROM modules m WHERE m.code = 'SALES'
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions(id, code, name, group_id, description, created_at)
SELECT (substr(md5('permission:' || p.code),1,8) || '-' || substr(md5('permission:' || p.code),9,4) || '-' ||
        substr(md5('permission:' || p.code),13,4) || '-' || substr(md5('permission:' || p.code),17,4) || '-' ||
        substr(md5('permission:' || p.code),21,12))::uuid,
       p.code, p.name, g.id, p.description, current_timestamp
FROM permission_groups g
CROSS JOIN (VALUES
    ('RETURN_VIEW', 'Xem yêu cầu đổi trả', 'Xem yêu cầu đổi / trả hàng của cửa hàng'),
    ('RETURN_MANAGE', 'Xử lý đổi trả', 'Duyệt, từ chối, nhận hàng và hoàn tất đổi / trả, xác nhận đã hoàn tiền')
) AS p(code, name, description)
WHERE g.code = 'RETURN'
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r CROSS JOIN permissions p
WHERE (r.code IN ('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'STAFF') AND p.code IN ('RETURN_VIEW', 'RETURN_MANAGE'))
   OR (r.code = 'ACCOUNTANT' AND p.code = 'RETURN_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Chính sách đổi trả cho bot hỗ trợ (trước đây là "đang cập nhật").
UPDATE support_faqs
   SET answer = 'Bạn có thể đổi size/màu (cùng sản phẩm) hoặc trả hàng trong 14 ngày kể từ khi đơn hoàn thành. '
             || 'Vào chi tiết đơn hàng, chọn "Yêu cầu đổi/trả", chọn sản phẩm và lý do. Sau khi cửa hàng duyệt, '
             || 'vui lòng mang sản phẩm tới cửa hàng đã bán để nhân viên kiểm tra. Đơn trả tiền mặt/COD được hoàn tiền '
             || 'tại quầy; đơn thanh toán VNPay/MoMo được hoàn về tài khoản thanh toán. Điểm tích lũy của phần hoàn tiền '
             || 'sẽ bị trừ lại.',
       updated_at = now() AT TIME ZONE 'UTC'
 WHERE topic = 'RETURN';
