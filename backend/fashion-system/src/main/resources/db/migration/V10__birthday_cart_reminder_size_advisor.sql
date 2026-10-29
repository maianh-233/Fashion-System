-- Voucher sinh nhật: admin tạo khuyến mãi loại BIRTHDAY (mức giảm, đơn tối thiểu, giảm tối đa, hạng áp dụng).
-- Ngày 1 tháng sinh, BirthdayVoucherScheduler phát cho mỗi khách một voucher riêng (customer_promotions,
-- source BIRTHDAY) dùng trong cả tháng đó, online và tại quầy; mỗi khách tối đa một voucher mỗi năm.

ALTER TABLE promotions DROP CONSTRAINT chk_promotions_type;
ALTER TABLE promotions ADD CONSTRAINT chk_promotions_type
    CHECK (promotion_type IN ('ORDER', 'PRODUCT', 'POINT_REDEMPTION', 'BIRTHDAY'));

ALTER TABLE customer_promotions DROP CONSTRAINT customer_promotions_points_spent_check;
ALTER TABLE customer_promotions
    ADD COLUMN source        VARCHAR(20) NOT NULL DEFAULT 'POINTS',
    ADD COLUMN valid_from    TIMESTAMP,
    ADD COLUMN expires_at    TIMESTAMP,
    ADD COLUMN birthday_year INTEGER;
ALTER TABLE customer_promotions ADD CONSTRAINT chk_customer_promotions_source CHECK (
    (source = 'POINTS' AND points_spent > 0 AND birthday_year IS NULL)
    OR (source = 'BIRTHDAY' AND points_spent = 0 AND birthday_year IS NOT NULL
        AND valid_from IS NOT NULL AND expires_at > valid_from));
CREATE UNIQUE INDEX uq_customer_promotions_birthday ON customer_promotions (customer_id, birthday_year)
    WHERE source = 'BIRTHDAY';

-- Nhắc giỏ hàng: giỏ không đổi 24 giờ thì nhắc một lần (thông báo + email). cart_updated_at là lần đổi giỏ
-- cuối cùng đã được nhắc; chỉ nhắc lại khi giỏ có thay đổi mới hơn.
CREATE TABLE cart_reminders (
    customer_id     UUID PRIMARY KEY REFERENCES customers(id) ON DELETE CASCADE,
    cart_updated_at TIMESTAMP NOT NULL,
    reminded_at     TIMESTAMP NOT NULL
);

-- Gợi ý size: bảng size theo danh mục (danh mục con dùng bảng của danh mục cha gần nhất nếu không có riêng)
-- và số đo khách tự lưu trong hồ sơ.
CREATE TABLE size_chart_entries (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id   UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    size          VARCHAR(50) NOT NULL,
    min_height_cm SMALLINT,
    max_height_cm SMALLINT,
    min_weight_kg NUMERIC(5, 1),
    max_weight_kg NUMERIC(5, 1),
    sort_order    INTEGER NOT NULL,
    CONSTRAINT chk_size_chart_height CHECK (min_height_cm IS NULL OR max_height_cm IS NULL OR min_height_cm <= max_height_cm),
    CONSTRAINT chk_size_chart_weight CHECK (min_weight_kg IS NULL OR max_weight_kg IS NULL OR min_weight_kg <= max_weight_kg),
    CONSTRAINT chk_size_chart_has_range CHECK (num_nonnulls(min_height_cm, max_height_cm, min_weight_kg, max_weight_kg) > 0)
);
CREATE UNIQUE INDEX uq_size_chart_entries_size ON size_chart_entries (category_id, lower(trim(size)));

ALTER TABLE customers
    ADD COLUMN height_cm SMALLINT CHECK (height_cm BETWEEN 80 AND 250),
    ADD COLUMN weight_kg NUMERIC(5, 1) CHECK (weight_kg BETWEEN 20 AND 250);
