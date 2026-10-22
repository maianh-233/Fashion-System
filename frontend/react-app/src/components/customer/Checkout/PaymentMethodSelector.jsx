import { Check, CreditCard, Landmark, Smartphone, Wallet } from "lucide-react";

const DETAILS = {
  COD: { description: "Thanh toán tiền mặt cho tài xế khi nhận hàng", icon: Wallet },
  VNPAY: { description: "Thẻ ATM, Visa, Mastercard hoặc QR · thanh toán trong 15 phút", icon: CreditCard },
  MOMO: { description: "Thanh toán qua ứng dụng MoMo · thanh toán trong 15 phút", icon: Smartphone },
};

/** Phương thức thanh toán từ API; cổng chưa sẵn sàng hiển thị nhưng không chọn được. */
export default function PaymentMethodSelector({ methods = [], value, onChange, eyebrow = "Bước 04" }) {
  return (
    <section className="checkout-section checkout-payment">
      <div className="checkout-section__heading">
        <span><Landmark size={17} /></span>
        <div><small>{eyebrow}</small><h2>Phương thức thanh toán</h2></div>
      </div>

      <div className="checkout-payment__list">
        {methods.map((method) => {
          const detail = DETAILS[method.code] || { description: "", icon: Wallet };
          const Icon = detail.icon;
          const active = value === method.code;
          return (
            <label key={method.code} className={`${active ? "is-active" : ""} ${method.available ? "" : "is-disabled"}`}>
              <input type="radio" name="payment-method" checked={active} disabled={!method.available}
                onChange={() => onChange(method.code)} />
              <Icon size={19} />
              <span>
                <strong>{method.label}</strong>
                <small>{method.available ? detail.description : "Tạm thời chưa hỗ trợ"}</small>
              </span>
              <i>{active && <Check size={13} />}</i>
            </label>
          );
        })}
      </div>
    </section>
  );
}
