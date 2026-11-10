-- Chiến dịch marketing — Phase 4:
--  1. Email marketing có đồng ý nhận tin (opt-in, mặc định KHÔNG) và hủy đăng ký bằng link; khách tự tắt thông báo
--     marketing trong app. Lưu trong bảng riêng của module (không ALTER bảng customers); mọi lần đồng ý / rút lại được ghi
--     lịch sử để chứng minh sự đồng ý (NĐ 13/2023 về dữ liệu cá nhân, NĐ 91/2020 về tin nhắn / thư điện tử quảng cáo).
--  2. Nhóm đối chứng (holdout): một tỷ lệ người nhận được giữ lại không gửi để đo mức tăng thêm.
--  3. Chiến dịch theo cửa hàng: store_id null = toàn chuỗi; quản lý cửa hàng tự lập chiến dịch cho cửa hàng mình.
--  (ROI theo giá vốn chỉ đọc dữ liệu phiếu nhập có sẵn, không cần bảng mới.)

CREATE TABLE marketing_customer_preferences (
    customer_id            UUID PRIMARY KEY REFERENCES customers(id) ON DELETE CASCADE,
    email_opt_in           BOOLEAN NOT NULL DEFAULT false,
    email_consented_at     TIMESTAMP,
    email_unsubscribed_at  TIMESTAMP,
    in_app_opt_out         BOOLEAN NOT NULL DEFAULT false,
    -- Token trong link hủy đăng ký của mọi email; ngẫu nhiên 122 bit, không đoán được.
    unsubscribe_token      UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    updated_at             TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);

CREATE TABLE marketing_consent_events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id  UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    channel      VARCHAR(20) NOT NULL CHECK (channel IN ('EMAIL', 'IN_APP')),
    action       VARCHAR(20) NOT NULL CHECK (action IN ('GRANTED', 'WITHDRAWN')),
    source       VARCHAR(30) NOT NULL CHECK (source IN ('CUSTOMER_PROFILE', 'UNSUBSCRIBE_LINK')),
    created_at   TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);
CREATE INDEX idx_marketing_consent_events_customer ON marketing_consent_events (customer_id, created_at DESC);

-- Đợt gửi: thêm kênh EMAIL và tỷ lệ nhóm đối chứng.
ALTER TABLE marketing_dispatches DROP CONSTRAINT marketing_dispatches_channel_check;
ALTER TABLE marketing_dispatches ADD CONSTRAINT marketing_dispatches_channel_check CHECK (channel IN ('IN_APP', 'EMAIL'));
ALTER TABLE marketing_dispatches ADD COLUMN holdout_percent INTEGER NOT NULL DEFAULT 0
    CONSTRAINT chk_marketing_dispatches_holdout CHECK (holdout_percent BETWEEN 0 AND 50);
ALTER TABLE marketing_dispatches ADD COLUMN holdout_count INTEGER;

-- Người nhận: trạng thái HOLDOUT (thuộc nhóm đối chứng, không gửi), lý do bỏ qua mới, email chốt lúc gửi.
ALTER TABLE marketing_dispatch_recipients DROP CONSTRAINT marketing_dispatch_recipients_status_check;
ALTER TABLE marketing_dispatch_recipients ADD CONSTRAINT marketing_dispatch_recipients_status_check
    CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED', 'SKIPPED', 'HOLDOUT'));
ALTER TABLE marketing_dispatch_recipients DROP CONSTRAINT marketing_dispatch_recipients_skip_reason_check;
ALTER TABLE marketing_dispatch_recipients ADD CONSTRAINT marketing_dispatch_recipients_skip_reason_check
    CHECK (skip_reason IN ('NO_ACCOUNT', 'INACTIVE', 'FREQUENCY_CAP', 'CANCELLED', 'CAMPAIGN_NOT_ACTIVE', 'NO_EMAIL',
                           'NO_CONSENT', 'OPTED_OUT'));
ALTER TABLE marketing_dispatch_recipients ADD COLUMN email VARCHAR(255);

-- Chiến dịch theo cửa hàng.
ALTER TABLE marketing_campaigns ADD COLUMN store_id UUID REFERENCES stores(id);
CREATE INDEX idx_marketing_campaigns_store ON marketing_campaigns (store_id, status) WHERE store_id IS NOT NULL;

-- Quản lý cửa hàng được lập / quản lý chiến dịch của cửa hàng mình (code giới hạn theo cửa hàng); duyệt vẫn là cấp chuỗi.
INSERT INTO role_permissions(role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r CROSS JOIN permissions p
WHERE r.code = 'MANAGER' AND p.code = 'MARKETING_CAMPAIGN_MANAGE'
ON CONFLICT (role_id, permission_id) DO NOTHING;

UPDATE permissions SET description = 'Tạo, sửa, gửi duyệt, hủy chiến dịch, liên kết ưu đãi và nhập chi phí; nhân viên cửa hàng chỉ với chiến dịch của cửa hàng mình'
WHERE code = 'MARKETING_CAMPAIGN_MANAGE';
