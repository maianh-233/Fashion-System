import { Check, CircleX, Clock3, CreditCard, RotateCcw } from "lucide-react";

const STATUS_META = {
  PAID: { label: "Đã thanh toán", icon: Check },
  PENDING: { label: "Đang xử lý", icon: Clock3 },
  FAILED: { label: "Không thành công", icon: CircleX },
  REFUNDED: { label: "Đã hoàn tiền", icon: RotateCcw },
};

const formatPrice = (value) => `${Number(value).toLocaleString("vi-VN")} ₫`;
const formatDate = (value) => value ? new Date(value).toLocaleString("vi-VN") : "Chưa hoàn tất";

export default function PaymentHistory({ payments = [] }) {
  return (
    <section className="checkout-section order-payment-history">
      <div className="checkout-section__heading">
        <span><CreditCard size={17} /></span>
        <div><small>Transactions</small><h2>Lịch sử thanh toán</h2></div>
        <strong>{payments.length}</strong>
      </div>

      <div className="order-payment-history__list">
        {payments.map((payment) => {
          const meta = STATUS_META[payment.status] || STATUS_META.PENDING;
          const Icon = meta.icon;
          return (
            <article key={payment.id}>
              <span className="order-payment-history__icon"><Icon size={15} /></span>
              <div>
                <strong>{payment.payment_code}</strong>
                <p>{payment.method} · {formatDate(payment.paid_at || payment.created_at)}</p>
                {payment.transaction_code && <small>Mã giao dịch · {payment.transaction_code}</small>}
              </div>
              <div className="order-payment-history__amount">
                <strong>{formatPrice(payment.amount)}</strong>
                <span>{meta.label}</span>
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}
