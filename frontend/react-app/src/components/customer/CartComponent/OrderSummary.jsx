import { ArrowRight, ShieldCheck, Tag, Truck, X } from "lucide-react";
import { Link } from "react-router-dom";
import Button from "../../common/Button";
import PromoInput from "./PromoInput";

const formatPrice = (value) => `${Number(value).toLocaleString("vi-VN")} ₫`;

export default function OrderSummary({
  subtotal,
  total,
  selectedCount = 0,
  appliedPromos,
  onApplyPromo,
  onRemovePromo,
}) {
  return (
    <aside className="customer-order-summary cart-summary">
      <div className="cart-summary__heading">
        <p>Order summary</p>
        <h2>Tóm tắt đơn hàng</h2>
        <span>{selectedCount} sản phẩm được chọn</span>
      </div>

      <div className="cart-summary__promo">
        <div className="cart-summary__section-title"><Tag size={14} /> Mã ưu đãi</div>
        <PromoInput onApply={onApplyPromo} disabled={appliedPromos.length >= 3} />
      </div>

      {appliedPromos.length > 0 && (
        <div className="cart-summary__promo-list">
          {appliedPromos.map((promotion) => (
            <div key={promotion.code}>
              <span><Tag size={12} /> {promotion.code}</span>
              <strong>-{formatPrice(promotion.amount)}</strong>
              <Button type="button" variant="unstyled" onClick={() => onRemovePromo(promotion.code)} aria-label={`Xóa mã ${promotion.code}`}>
                <X size={14} />
              </Button>
            </div>
          ))}
        </div>
      )}

      <div className="cart-summary__totals">
        <div><span>Tạm tính</span><strong>{formatPrice(subtotal)}</strong></div>
        <div><span>Phí vận chuyển</span><strong>Miễn phí</strong></div>
        {appliedPromos.map((promotion) => (
          <div className="is-discount" key={promotion.code}>
            <span>Ưu đãi · {promotion.code}</span>
            <strong>-{formatPrice(promotion.amount)}</strong>
          </div>
        ))}
        <div className="cart-summary__grand-total">
          <span>Tổng thanh toán</span>
          <strong>{formatPrice(total)}</strong>
        </div>
      </div>

      <Link
        to="/checkout"
        className={`cart-checkout-button ${selectedCount === 0 ? "is-disabled" : ""}`}
        aria-disabled={selectedCount === 0}
        onClick={(event) => {
          if (selectedCount === 0) event.preventDefault();
        }}
      >
        Tiến hành thanh toán <ArrowRight size={16} />
      </Link>

      <div className="cart-summary__assurance">
        <span><ShieldCheck size={15} /> Thanh toán được bảo mật</span>
        <span><Truck size={15} /> Miễn phí giao hàng toàn quốc</span>
      </div>
    </aside>
  );
}
