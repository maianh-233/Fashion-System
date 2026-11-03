-- Hạn sử dụng điểm tích lũy: mỗi lần tích điểm là một đợt có hạn 12 tháng. Đổi voucher và trừ do hoàn tiền trừ vào
-- đợt cũ nhất trước (hoàn tiền trừ vào đợt của chính đơn đó trước); đến hạn, phần còn lại của đợt thành giao dịch EXPIRE.
-- Nhắc khách trước 30 ngày và 7 ngày. Tổng points_remaining của các đợt luôn bằng loyalty_accounts.points_balance.

CREATE TABLE loyalty_point_lots (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id      UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    order_id         UUID REFERENCES orders(id) ON DELETE SET NULL,
    points_earned    INTEGER NOT NULL CHECK (points_earned > 0),
    points_remaining INTEGER NOT NULL CHECK (points_remaining >= 0 AND points_remaining <= points_earned),
    points_expired   INTEGER NOT NULL DEFAULT 0 CHECK (points_expired >= 0),
    earned_at        TIMESTAMP NOT NULL,
    expires_at       TIMESTAMP NOT NULL CHECK (expires_at > earned_at),
    reminded_30_at   TIMESTAMP,
    reminded_7_at    TIMESTAMP,
    note             VARCHAR(200)
);

CREATE INDEX idx_loyalty_point_lots_open ON loyalty_point_lots (customer_id, expires_at) WHERE points_remaining > 0;
CREATE INDEX idx_loyalty_point_lots_due ON loyalty_point_lots (expires_at) WHERE points_remaining > 0;
CREATE UNIQUE INDEX uq_loyalty_point_lots_order ON loyalty_point_lots (order_id) WHERE order_id IS NOT NULL;

-- Số dư hiện có trở thành một đợt dùng được tới hết ngày này năm sau (giờ Việt Nam; cột lưu UTC): không ai mất điểm
-- ngay khi cập nhật.
INSERT INTO loyalty_point_lots (customer_id, points_earned, points_remaining, earned_at, expires_at, note)
SELECT customer_id, points_balance, points_balance, now() AT TIME ZONE 'UTC',
       (((now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date + INTERVAL '12 months' + INTERVAL '1 day')
           AT TIME ZONE 'Asia/Ho_Chi_Minh') AT TIME ZONE 'UTC',
       'Số dư lúc bắt đầu tính hạn điểm'
FROM loyalty_accounts
WHERE points_balance > 0;
