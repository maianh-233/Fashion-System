-- Chiến dịch voucher theo nhóm khách: admin chọn nhóm (hạng, lâu chưa mua, từng mua danh mục; thỏa tất cả tiêu chí),
-- nhập mức giảm và số ngày được dùng; hệ thống tạo một khuyến mãi loại CAMPAIGN làm mẫu rồi, lúc gửi (ngay hoặc theo
-- giờ hẹn), phát cho mỗi khách khớp nhóm một voucher riêng (customer_promotions, source CAMPAIGN) và báo qua thông báo
-- trong app + email. Mặc định bỏ qua khách đã nhận voucher chiến dịch trong 14 ngày.

ALTER TABLE promotions DROP CONSTRAINT chk_promotions_type;
ALTER TABLE promotions ADD CONSTRAINT chk_promotions_type
    CHECK (promotion_type IN ('ORDER', 'PRODUCT', 'POINT_REDEMPTION', 'BIRTHDAY', 'CAMPAIGN'));

CREATE SEQUENCE voucher_campaign_code_seq;

CREATE TABLE voucher_campaigns (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code               VARCHAR(30) NOT NULL UNIQUE,
    name               VARCHAR(150) NOT NULL,
    status             VARCHAR(20) NOT NULL CHECK (status IN ('SCHEDULED', 'SENT', 'CANCELLED')),
    promotion_id       UUID NOT NULL REFERENCES promotions(id),
    inactive_days      INTEGER CHECK (inactive_days > 0),
    category_id        UUID REFERENCES categories(id) ON DELETE SET NULL,
    skip_recent_days   INTEGER CHECK (skip_recent_days > 0),
    valid_days         INTEGER NOT NULL CHECK (valid_days BETWEEN 1 AND 365),
    message            VARCHAR(500),
    scheduled_at       TIMESTAMP NOT NULL,
    sent_at            TIMESTAMP,
    recipient_count    INTEGER,
    created_by         UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    cancelled_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    cancelled_at       TIMESTAMP
);

CREATE INDEX idx_voucher_campaigns_due ON voucher_campaigns (scheduled_at) WHERE status = 'SCHEDULED';
CREATE INDEX idx_voucher_campaigns_created ON voucher_campaigns (created_at DESC);

CREATE TABLE voucher_campaign_tiers (
    campaign_id UUID NOT NULL REFERENCES voucher_campaigns(id) ON DELETE CASCADE,
    tier_id     UUID NOT NULL REFERENCES customer_tiers(id) ON DELETE CASCADE,
    PRIMARY KEY (campaign_id, tier_id)
);

ALTER TABLE customer_promotions ADD COLUMN campaign_id UUID REFERENCES voucher_campaigns(id);
ALTER TABLE customer_promotions DROP CONSTRAINT chk_customer_promotions_source;
ALTER TABLE customer_promotions ADD CONSTRAINT chk_customer_promotions_source CHECK (
    (source = 'POINTS' AND points_spent > 0 AND birthday_year IS NULL AND campaign_id IS NULL)
    OR (source = 'BIRTHDAY' AND points_spent = 0 AND birthday_year IS NOT NULL AND campaign_id IS NULL
        AND valid_from IS NOT NULL AND expires_at > valid_from)
    OR (source = 'CAMPAIGN' AND points_spent = 0 AND birthday_year IS NULL AND campaign_id IS NOT NULL
        AND valid_from IS NOT NULL AND expires_at > valid_from));
CREATE UNIQUE INDEX uq_customer_promotions_campaign ON customer_promotions (campaign_id, customer_id)
    WHERE source = 'CAMPAIGN';
CREATE INDEX idx_customer_promotions_campaign_recent ON customer_promotions (customer_id, redeemed_at)
    WHERE source = 'CAMPAIGN';
