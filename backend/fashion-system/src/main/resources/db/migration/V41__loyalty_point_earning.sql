-- Tích điểm theo đơn hàng: 1.000đ tiền hàng khách trả = 1 điểm, cộng khi đơn hoàn tất, trừ lại khi hoàn tiền.
-- amount: số tiền làm căn cứ (dương khi tích điểm, âm khi hoàn tiền) để cập nhật tổng chi tiêu.
ALTER TABLE loyalty_transactions ADD COLUMN IF NOT EXISTS amount NUMERIC(14, 2);

-- Mỗi đơn chỉ được tích điểm một lần.
CREATE UNIQUE INDEX IF NOT EXISTS uq_loyalty_transactions_order_earn
    ON loyalty_transactions (reference_id) WHERE transaction_type = 'EARN' AND reference_type = 'ORDER';

CREATE INDEX IF NOT EXISTS idx_loyalty_transactions_customer_created
    ON loyalty_transactions (customer_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_loyalty_transactions_reference
    ON loyalty_transactions (reference_type, reference_id);

-- Thêm loại REFUND_REVERSAL (trừ điểm khi hoàn tiền). Dòng 0 điểm chỉ hợp lệ khi có số tiền: đơn dưới 1.000đ vẫn
-- ghi để không tích lại, và hoàn tiền khi số dư đã về 0 vẫn ghi để trừ tổng chi tiêu.
ALTER TABLE loyalty_transactions DROP CONSTRAINT IF EXISTS loyalty_transactions_transaction_type_check;
ALTER TABLE loyalty_transactions ADD CONSTRAINT loyalty_transactions_transaction_type_check
    CHECK (transaction_type IN ('EARN', 'REDEEM', 'EXPIRE', 'ADJUST', 'REFUND_REVERSAL'));
ALTER TABLE loyalty_transactions DROP CONSTRAINT IF EXISTS loyalty_transactions_points_check;
ALTER TABLE loyalty_transactions ADD CONSTRAINT loyalty_transactions_points_check
    CHECK (points <> 0 OR amount IS NOT NULL);
