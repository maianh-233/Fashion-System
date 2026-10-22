import { Check, CircleX, Clock3, CreditCard } from "lucide-react";
import { formatPrice } from "../../../pages/customer/storefrontLogic";
import { PAYMENT_ATTEMPT_LABELS, PAYMENT_METHOD_LABELS, formatDateTime } from "../../../pages/customer/customerOrderLogic";

const ICONS = { SUCCESS: Check, FAILED: CircleX, PENDING: Clock3 };

/** Các lần thanh toán của đơn (COD được ghi khi giao thành công). */
export default function PaymentHistory({ payments = [], method }) {
  return (
    <section className="checkout-section order-payment-history">
      <div className="checkout-section__heading">
        <span><CreditCard size={17} /></span>
        <div><small>Transactions</small><h2>Lịch sử thanh toán</h2></div>
        <strong>{payments.length}</strong>
      </div>

      {payments.length === 0 && (
        <p className="checkout-muted">
          {method === "COD" ? "Bạn thanh toán cho tài xế khi nhận hàng; giao dịch được ghi nhận khi giao thành công." : "Chưa có giao dịch nào."}
        </p>
      )}
      <div className="order-payment-history__list">
        {payments.map((payment, index) => {
          const Icon = ICONS[payment.status] || Clock3;
          return (
            <article key={`${payment.createdAt}-${index}`}>
              <span className="order-payment-history__icon"><Icon size={15} /></span>
              <div>
                <strong>{PAYMENT_METHOD_LABELS[payment.method] || payment.method}</strong>
                <p>{formatDateTime(payment.paidAt || payment.createdAt)}</p>
                {payment.failureReason && <small>{payment.failureReason}</small>}
              </div>
              <div className="order-payment-history__amount">
                <strong>{formatPrice(payment.amount)}</strong>
                <span>{PAYMENT_ATTEMPT_LABELS[payment.status] || payment.status}</span>
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}
