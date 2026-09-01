import { ArrowRight, LockKeyhole, ShieldCheck } from "lucide-react";
import Button from "../../common/Button";

const formatPrice = (value) => `${Number(value).toLocaleString("vi-VN")} ₫`;

export default function PriceSummary({ order, readOnly = false }) {
  if (!order) return null;

  const total = order.subtotal - order.discount_total + order.shipping_fee + order.tax;

  return (
    <div className="checkout-price-summary">
      <div className="checkout-price-summary__rows">
        <Row label="Tạm tính" value={order.subtotal} />
        <Row label="Giảm giá" value={-order.discount_total} discount />
        <Row label="Phí vận chuyển" value={order.shipping_fee} />
        <Row label="Thuế" value={order.tax} />
      </div>

      <div className="checkout-price-summary__total">
        <span>Tổng thanh toán<small>Đã bao gồm thuế và phí</small></span>
        <strong>{formatPrice(total)}</strong>
      </div>

      {!readOnly && (
        <Button type="button" variant="unstyled" className="checkout-place-order">
          Đặt hàng <ArrowRight size={16} />
        </Button>
      )}

      <div className="checkout-price-summary__secure">
        <span><LockKeyhole size={14} /> {readOnly ? "Thông tin thanh toán đã được ghi nhận" : "Dữ liệu thanh toán được mã hóa"}</span>
        <span><ShieldCheck size={14} /> Chính sách đổi trả trong 14 ngày</span>
      </div>
    </div>
  );
}

function Row({ label, value, discount = false }) {
  return (
    <div className={discount ? "is-discount" : ""}>
      <span>{label}</span>
      <strong>{value < 0 ? "−" : ""}{formatPrice(Math.abs(value))}</strong>
    </div>
  );
}
