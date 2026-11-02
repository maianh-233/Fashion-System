-- Báo cho khách khi sản phẩm yêu thích giảm giá (giá thấp nhất đang bán giảm từ 5%), vào flash sale hoặc sắp hết hàng
-- (tổng tồn online từ 1 đến 5). WishlistAlertService quét định kỳ: thông báo trong app gửi ngay theo từng sản phẩm,
-- email gộp tối đa 1 lần mỗi 24 giờ. Khách tắt được trong hồ sơ (mặc định bật).

ALTER TABLE customers ADD COLUMN wishlist_alerts_enabled BOOLEAN NOT NULL DEFAULT true;

-- Mốc đã báo của từng sản phẩm yêu thích: giá làm mốc so sánh (null = lần quét tới chỉ ghi mốc, không báo), đợt flash
-- sale đã báo gần nhất, và lúc đã báo sắp hết hàng (xóa khi tồn lại vượt ngưỡng để lần sau báo lại).
ALTER TABLE wishlist_items
    ADD COLUMN baseline_price         NUMERIC(12, 2),
    ADD COLUMN notified_flash_sale_id UUID REFERENCES flash_sales(id) ON DELETE SET NULL,
    ADD COLUMN low_stock_notified_at  TIMESTAMP;

-- Sự kiện đã báo, chờ gộp vào email (emailed_at null = chưa gửi email). Giữ 30 ngày.
CREATE TABLE wishlist_alert_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    product_id  UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    event_type  VARCHAR(20) NOT NULL CHECK (event_type IN ('PRICE_DROP', 'FLASH_SALE', 'LOW_STOCK')),
    message     VARCHAR(500) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    emailed_at  TIMESTAMP
);

CREATE INDEX idx_wishlist_alert_events_pending ON wishlist_alert_events (customer_id) WHERE emailed_at IS NULL;
CREATE INDEX idx_wishlist_alert_events_emailed ON wishlist_alert_events (customer_id, emailed_at DESC);
CREATE INDEX idx_wishlist_alert_events_created ON wishlist_alert_events (created_at);
