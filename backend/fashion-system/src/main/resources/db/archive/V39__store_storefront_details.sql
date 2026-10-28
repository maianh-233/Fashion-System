-- Thông tin cửa hàng hiển thị cho khách (trang hệ thống cửa hàng): giờ mở cửa, ảnh, dịch vụ. Đều tùy chọn.
ALTER TABLE stores ADD COLUMN IF NOT EXISTS opening_hours VARCHAR(100);
ALTER TABLE stores ADD COLUMN IF NOT EXISTS image_url VARCHAR(1000);
ALTER TABLE stores ADD COLUMN IF NOT EXISTS image_public_id VARCHAR(255);
ALTER TABLE stores ADD COLUMN IF NOT EXISTS services TEXT[] NOT NULL DEFAULT '{}';

-- Trang sản phẩm tra cửa hàng còn hàng theo biến thể (tồn tại quầy).
CREATE INDEX IF NOT EXISTS idx_inventory_balances_variant_offline
    ON inventory_balances (product_variant_id) WHERE available_quantity > 0;
