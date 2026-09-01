import { Check, CreditCard, Landmark, Smartphone, Wallet } from "lucide-react";

const methods = [
  { value: "COD", title: "Thanh toán khi nhận hàng", description: "Thanh toán trực tiếp cho đơn vị vận chuyển", icon: Wallet },
  { value: "VNPAY", title: "VNPAY", description: "Thẻ ATM, Visa, Mastercard hoặc QR", icon: CreditCard },
  { value: "MOMO", title: "Ví MoMo", description: "Thanh toán nhanh qua ứng dụng MoMo", icon: Smartphone },
];

export default function PaymentMethodSelector({ value, onChange }) {
  return (
    <section className="checkout-section checkout-payment">
      <div className="checkout-section__heading">
        <span><Landmark size={17} /></span>
        <div><small>Bước 04</small><h2>Phương thức thanh toán</h2></div>
      </div>

      <div className="checkout-payment__list">
        {methods.map((method) => {
          const Icon = method.icon;
          const active = value === method.value;
          return (
            <label key={method.value} className={active ? "is-active" : ""}>
              <input type="radio" name="payment-method" checked={active} onChange={() => onChange(method.value)} />
              <Icon size={19} />
              <span><strong>{method.title}</strong><small>{method.description}</small></span>
              <i>{active && <Check size={13} />}</i>
            </label>
          );
        })}
      </div>
    </section>
  );
}
