import { LockKeyhole, ShieldCheck } from "lucide-react";
import { formatPrice } from "../../../pages/customer/storefrontLogic";

/** Tổng tiền do backend tính (tạm tính, giảm giá, phí ship, tổng); {@code children} là nút thao tác. */
export default function PriceSummary({ subtotal = 0, discount = 0, shippingFee = 0, total = 0, readOnly = false, children }) {
  return (
    <div className="checkout-price-summary">
      <div className="checkout-price-summary__rows">
        <Row label="Tạm tính" value={subtotal} />
        <Row label="Giảm giá" value={-Number(discount || 0)} discount />
        <Row label="Phí vận chuyển" value={shippingFee} free={Number(shippingFee || 0) === 0} />
      </div>

      <div className="checkout-price-summary__total">
        <span>Tổng thanh toán<small>Đã gồm giảm giá và phí giao hàng</small></span>
        <strong>{formatPrice(total)}</strong>
      </div>

      {children}

      <div className="checkout-price-summary__secure">
        <span><LockKeyhole size={14} /> {readOnly ? "Thông tin thanh toán đã được ghi nhận" : "Giá và tồn kho được kiểm tra lại khi đặt hàng"}</span>
        <span><ShieldCheck size={14} /> Bạn có thể tự hủy khi cửa hàng chưa xác nhận đơn</span>
      </div>
    </div>
  );
}

function Row({ label, value, discount = false, free = false }) {
  const amount = Number(value || 0);
  return (
    <div className={discount ? "is-discount" : ""}>
      <span>{label}</span>
      <strong>{free ? "Miễn phí" : `${amount < 0 ? "−" : ""}${formatPrice(Math.abs(amount))}`}</strong>
    </div>
  );
}
