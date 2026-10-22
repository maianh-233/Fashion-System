import { ArrowRight, ShieldCheck, Tag, Truck } from "lucide-react";
import { Link } from "react-router-dom";
import { formatPrice } from "../../../pages/customer/storefrontLogic";

/** Tóm tắt giỏ: tạm tính các dòng được chọn; voucher và phân cửa hàng tính ở bước thanh toán. */
export default function OrderSummary({ subtotal, selectedCount = 0, canCheckout, loggedIn, blockedReason }) {
  const target = loggedIn ? "/checkout" : `/customerlogin?redirect=${encodeURIComponent("/checkout")}`;
  return (
    <aside className="customer-order-summary cart-summary">
      <div className="cart-summary__heading">
        <p>Order summary</p>
        <h2>Tóm tắt đơn hàng</h2>
        <span>{selectedCount} sản phẩm được chọn</span>
      </div>

      <div className="cart-summary__promo">
        <div className="cart-summary__section-title"><Tag size={14} /> Mã ưu đãi</div>
        <p className="cart-promo-message">Chọn hoặc nhập mã ưu đãi ở bước thanh toán, sau khi chọn địa chỉ giao hàng.</p>
      </div>

      <div className="cart-summary__totals">
        <div><span>Tạm tính</span><strong>{formatPrice(subtotal)}</strong></div>
        <div><span>Phí vận chuyển</span><strong>Tính ở bước thanh toán</strong></div>
        <div className="cart-summary__grand-total">
          <span>Tạm tính</span>
          <strong>{formatPrice(subtotal)}</strong>
        </div>
      </div>

      {blockedReason && <p className="cart-promo-message is-error">{blockedReason}</p>}
      <Link
        to={target}
        className={`cart-checkout-button ${canCheckout ? "" : "is-disabled"}`}
        aria-disabled={!canCheckout}
        onClick={(event) => {
          if (!canCheckout) event.preventDefault();
        }}
      >
        {loggedIn ? "Tiến hành thanh toán" : "Đăng nhập để thanh toán"} <ArrowRight size={16} />
      </Link>

      <div className="cart-summary__assurance">
        <span><ShieldCheck size={15} /> Thanh toán được bảo mật</span>
        <span><Truck size={15} /> Giao từ cửa hàng gần bạn nhất còn hàng</span>
      </div>
    </aside>
  );
}
