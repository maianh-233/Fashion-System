-- Ưu đãi hạng thành viên tự áp vào đơn (online và tại quầy): phần trăm của hạng hiện tại tính trên phần còn lại của
-- từng dòng sau flash sale / khuyến mãi sản phẩm, trước voucher toàn đơn. Số tiền đã nằm trong order_items.discount_amount
-- và orders.discount_total; các cột dưới đây ghi lại hạng và phần trăm lúc đặt để hiển thị. Lưu ở đơn khách đặt (đơn tổng
-- khi đơn bị tách). Đơn cũ để trống.
ALTER TABLE orders
    ADD COLUMN tier_name             VARCHAR(100),
    ADD COLUMN tier_discount_percent NUMERIC(5, 2),
    ADD COLUMN tier_discount         NUMERIC(14, 2);
