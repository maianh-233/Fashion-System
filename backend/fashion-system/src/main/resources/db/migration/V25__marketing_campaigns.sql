-- Quản lý chiến dịch marketing (Phase 1): chiến dịch là lớp điều phối phía trên Promotion, Voucher Campaign và Flash Sale
-- có sẵn — thêm mục tiêu, thời gian, ngân sách, quy trình duyệt, chi phí thực tế và lịch sử. Module không tạo / sửa / tắt
-- ưu đãi; chỉ liên kết tới chúng bằng khóa ngoại từ bảng mới, không ALTER bảng nào của module khác.
-- Trạng thái lưu: DRAFT → PENDING_APPROVAL → APPROVED / REJECTED, hủy được từ mọi trạng thái. Giai đoạn
-- Sắp diễn ra / Đang chạy / Đã kết thúc của chiến dịch APPROVED tính từ starts_at / ends_at, không lưu.
-- Tên bảng có tiền tố marketing_ để không nhầm với recruitment_campaigns (tuyển dụng) và voucher_campaigns.

CREATE SEQUENCE marketing_campaign_code_seq;

CREATE TABLE marketing_campaigns (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code             VARCHAR(30) NOT NULL UNIQUE,
    name             VARCHAR(150) NOT NULL,
    objective        VARCHAR(20) NOT NULL
        CHECK (objective IN ('REVENUE', 'ACQUISITION', 'RETENTION', 'CLEARANCE', 'AWARENESS')),
    description      TEXT,
    starts_at        TIMESTAMP NOT NULL,
    ends_at          TIMESTAMP NOT NULL,
    owner_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED')),
    planned_budget   NUMERIC(14, 2) CHECK (planned_budget >= 0),
    target_revenue   NUMERIC(14, 2) CHECK (target_revenue >= 0),
    target_orders    INTEGER CHECK (target_orders >= 0),
    target_buyers    INTEGER CHECK (target_buyers >= 0),
    submitted_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    submitted_at     TIMESTAMP,
    decided_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    decided_at       TIMESTAMP,
    decision_note    VARCHAR(500),
    cancelled_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    cancelled_at     TIMESTAMP,
    cancel_reason    VARCHAR(500),
    created_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at       TIMESTAMP,
    CONSTRAINT chk_marketing_campaigns_window CHECK (ends_at > starts_at)
);
CREATE INDEX idx_marketing_campaigns_status ON marketing_campaigns (status, starts_at);
CREATE INDEX idx_marketing_campaigns_window ON marketing_campaigns (starts_at, ends_at);
CREATE INDEX idx_marketing_campaigns_created ON marketing_campaigns (created_at DESC);

-- Một bảng cho ba loại ưu đãi: đúng một khóa ngoại được điền và khớp offer_type. Mỗi ưu đãi thuộc tối đa một chiến dịch
-- (kể cả chiến dịch đã hủy, cho tới khi gỡ liên kết) — unique riêng trên từng cột.
CREATE TABLE marketing_campaign_offers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id         UUID NOT NULL REFERENCES marketing_campaigns(id) ON DELETE CASCADE,
    offer_type          VARCHAR(20) NOT NULL CHECK (offer_type IN ('PROMOTION', 'VOUCHER_CAMPAIGN', 'FLASH_SALE')),
    promotion_id        UUID REFERENCES promotions(id),
    voucher_campaign_id UUID REFERENCES voucher_campaigns(id),
    flash_sale_id       UUID REFERENCES flash_sales(id),
    linked_by           UUID REFERENCES users(id) ON DELETE SET NULL,
    linked_at           TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_marketing_campaign_offers_one CHECK (
        (offer_type = 'PROMOTION' AND promotion_id IS NOT NULL AND voucher_campaign_id IS NULL AND flash_sale_id IS NULL)
        OR (offer_type = 'VOUCHER_CAMPAIGN' AND voucher_campaign_id IS NOT NULL AND promotion_id IS NULL AND flash_sale_id IS NULL)
        OR (offer_type = 'FLASH_SALE' AND flash_sale_id IS NOT NULL AND promotion_id IS NULL AND voucher_campaign_id IS NULL))
);
CREATE INDEX idx_marketing_campaign_offers_campaign ON marketing_campaign_offers (campaign_id, linked_at);
CREATE UNIQUE INDEX uq_marketing_campaign_offers_promotion ON marketing_campaign_offers (promotion_id)
    WHERE promotion_id IS NOT NULL;
CREATE UNIQUE INDEX uq_marketing_campaign_offers_voucher ON marketing_campaign_offers (voucher_campaign_id)
    WHERE voucher_campaign_id IS NOT NULL;
CREATE UNIQUE INDEX uq_marketing_campaign_offers_flash ON marketing_campaign_offers (flash_sale_id)
    WHERE flash_sale_id IS NOT NULL;

-- Chi phí thực tế ngoài hệ thống (quảng cáo, KOL, in ấn, sự kiện); nhập được ở mọi trạng thái vì hóa đơn thường về sau.
CREATE TABLE marketing_campaign_expenses (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id  UUID NOT NULL REFERENCES marketing_campaigns(id) ON DELETE CASCADE,
    category     VARCHAR(20) NOT NULL CHECK (category IN ('ADS', 'KOL', 'PRINTING', 'EVENT', 'OTHER')),
    amount       NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    spent_on     DATE NOT NULL,
    vendor       VARCHAR(150),
    note         VARCHAR(500),
    created_by   UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at   TIMESTAMP
);
CREATE INDEX idx_marketing_campaign_expenses_campaign ON marketing_campaign_expenses (campaign_id, spent_on DESC);

CREATE TABLE marketing_campaign_history (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id  UUID NOT NULL REFERENCES marketing_campaigns(id) ON DELETE CASCADE,
    action       VARCHAR(30) NOT NULL CHECK (action IN ('CREATED', 'UPDATED', 'SUBMITTED', 'WITHDRAWN', 'APPROVED',
                     'REJECTED', 'REOPENED', 'CANCELLED', 'OFFER_LINKED', 'OFFER_UNLINKED', 'EXPENSE_ADDED',
                     'EXPENSE_UPDATED', 'EXPENSE_DELETED')),
    from_status  VARCHAR(20),
    to_status    VARCHAR(20),
    note         VARCHAR(1000),
    actor_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);
CREATE INDEX idx_marketing_campaign_history_campaign ON marketing_campaign_history (campaign_id, created_at DESC);

-- ========== MODULE, QUYỀN VÀ VAI TRÒ ==========

INSERT INTO modules(id, active, code, created_at, description, icon, name, sort_order)
SELECT (substr(md5('module:MARKETING'),1,8) || '-' || substr(md5('module:MARKETING'),9,4) || '-' ||
        substr(md5('module:MARKETING'),13,4) || '-' || substr(md5('module:MARKETING'),17,4) || '-' ||
        substr(md5('module:MARKETING'),21,12))::uuid,
       true, 'MARKETING', current_timestamp, 'Lập kế hoạch, duyệt và theo dõi chiến dịch marketing', 'megaphone', 'Marketing', 35
ON CONFLICT DO NOTHING;

INSERT INTO permission_groups(id, name, code, description, created_at, module_id)
SELECT (substr(md5('permission_group:MARKETING_CAMPAIGN'),1,8) || '-' || substr(md5('permission_group:MARKETING_CAMPAIGN'),9,4) || '-' ||
        substr(md5('permission_group:MARKETING_CAMPAIGN'),13,4) || '-' || substr(md5('permission_group:MARKETING_CAMPAIGN'),17,4) || '-' ||
        substr(md5('permission_group:MARKETING_CAMPAIGN'),21,12))::uuid,
       'Chiến dịch marketing', 'MARKETING_CAMPAIGN', 'Chiến dịch, ưu đãi liên kết, ngân sách và quy trình duyệt', current_timestamp, m.id
FROM modules m WHERE m.code = 'MARKETING'
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions(id, code, name, group_id, description, created_at)
SELECT (substr(md5('permission:' || p.code),1,8) || '-' || substr(md5('permission:' || p.code),9,4) || '-' ||
        substr(md5('permission:' || p.code),13,4) || '-' || substr(md5('permission:' || p.code),17,4) || '-' ||
        substr(md5('permission:' || p.code),21,12))::uuid,
       p.code, p.name, g.id, p.description, current_timestamp
FROM permission_groups g
CROSS JOIN (VALUES
    ('MARKETING_CAMPAIGN_VIEW', 'Xem chiến dịch marketing',
     'Xem chiến dịch; nhân viên cửa hàng chỉ thấy chiến dịch đã duyệt và không thấy số liệu tài chính'),
    ('MARKETING_CAMPAIGN_MANAGE', 'Quản lý chiến dịch marketing',
     'Tạo, sửa, gửi duyệt, hủy chiến dịch, liên kết ưu đãi và nhập chi phí (chỉ nhân viên cấp chuỗi)'),
    ('MARKETING_CAMPAIGN_APPROVE', 'Duyệt chiến dịch marketing',
     'Duyệt hoặc từ chối chiến dịch; không tự duyệt chiến dịch của mình trừ SUPER_ADMIN')
) AS p(code, name, description)
WHERE g.code = 'MARKETING_CAMPAIGN'
ON CONFLICT (code) DO NOTHING;

INSERT INTO roles(code, name, description) VALUES
    ('MARKETING', 'Marketing', 'Lập và theo dõi chiến dịch marketing; cần người có quyền duyệt phê duyệt')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r CROSS JOIN permissions p
WHERE (r.code IN ('SUPER_ADMIN', 'ADMIN') AND p.code LIKE 'MARKETING\_CAMPAIGN\_%')
   OR (r.code = 'MARKETING' AND p.code IN ('MARKETING_CAMPAIGN_VIEW', 'MARKETING_CAMPAIGN_MANAGE'))
   -- Nhân viên cửa hàng và kế toán chỉ xem (MVP phạm vi toàn chuỗi).
   OR (r.code IN ('MANAGER', 'STAFF', 'ACCOUNTANT') AND p.code = 'MARKETING_CAMPAIGN_VIEW')
   -- Role Marketing cần xem khuyến mãi để chọn ưu đãi liên kết.
   OR (r.code = 'MARKETING' AND p.code = 'PROMOTION_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;
