-- The PAYMENT FAQ still said VNPay/MoMo were "coming soon" although both
-- gateways are live (OrderPaymentService, payment.hold-minutes = 15).
UPDATE support_faqs
   SET answer = 'Cửa hàng hỗ trợ thanh toán khi nhận hàng (COD), VNPay và ví MoMo. '
             || 'Với VNPay và MoMo, bạn cần hoàn tất thanh toán trong 15 phút sau khi đặt hàng; '
             || 'nếu thanh toán không thành công, bạn có thể tạo lại link thanh toán hoặc chuyển sang COD. '
             || 'Đơn quá hạn chưa thanh toán sẽ tự động bị hủy.',
       updated_at = now() AT TIME ZONE 'UTC'
 WHERE topic = 'PAYMENT';
