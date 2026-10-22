import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { CheckCircle2, Clock3, LoaderCircle, XCircle } from "lucide-react";
import { customerOrderApi } from "../../api/customerApi";

const POLL_MS = 3000;
const MAX_POLLS = 10;

/**
 * Trang khách quay về sau cổng thanh toán ({@code PAYMENT_RESULT_URL}). Kết quả trên URL chỉ để hiển thị; trạng thái
 * thật lấy từ đơn (IPN của cổng cập nhật), nên trang hỏi lại đơn vài lần cho tới khi thấy đã thanh toán.
 */
export default function PaymentResultPage() {
  const [params] = useSearchParams();
  const status = params.get("status");
  const orderId = params.get("orderId");
  const orderCode = params.get("orderCode");
  const message = params.get("message");
  const [paymentStatus, setPaymentStatus] = useState(null);
  const [polls, setPolls] = useState(0);

  useEffect(() => {
    if (!orderId || status !== "success" || paymentStatus === "PAID" || polls >= MAX_POLLS) return undefined;
    const timer = window.setTimeout(() => {
      customerOrderApi.detail(orderId)
        .then((order) => setPaymentStatus(order.paymentStatus))
        .catch(() => {})
        .finally(() => setPolls((value) => value + 1));
    }, polls === 0 ? 0 : POLL_MS);
    return () => window.clearTimeout(timer);
  }, [orderId, status, paymentStatus, polls]);

  const confirmed = paymentStatus === "PAID";
  const waiting = status === "success" && !confirmed;
  const view = status === "success"
    ? confirmed
      ? { icon: CheckCircle2, title: "Thanh toán thành công", text: `Đơn #${orderCode} đã được thanh toán và đang chờ cửa hàng xác nhận.` }
      : { icon: Clock3, title: "Đang xác nhận thanh toán", text: "Cổng thanh toán báo thành công, hệ thống đang ghi nhận giao dịch." }
    : status === "pending"
      ? { icon: Clock3, title: "Giao dịch đang xử lý", text: "Cổng thanh toán chưa có kết quả cuối cùng. Vui lòng kiểm tra lại đơn sau ít phút." }
      : status === "invalid"
        ? { icon: XCircle, title: "Không xác minh được kết quả", text: "Thông tin trả về từ cổng thanh toán không hợp lệ. Vui lòng kiểm tra đơn hàng của bạn." }
        : { icon: XCircle, title: "Thanh toán chưa thành công", text: message || "Bạn có thể thanh toán lại hoặc đổi sang thanh toán khi nhận hàng trong thời hạn giữ hàng." };
  const Icon = view.icon;

  return (
    <div className="customer-page min-h-screen w-full text-zinc-200">
      <div className="customer-page__wide px-4 py-16">
        <div className="payment-result">
          <Icon size={42} className={status === "success" ? "is-success" : status === "pending" || waiting ? "is-pending" : "is-failed"} />
          <h1>{view.title}</h1>
          <p>{view.text}</p>
          {waiting && polls < MAX_POLLS && <p className="payment-result__waiting"><LoaderCircle className="animate-spin" size={14} /> Đang kiểm tra...</p>}
          <div className="payment-result__actions">
            {orderId && <Link to={`/orderdetail?id=${orderId}`} className="checkout-primary-button">Xem đơn hàng</Link>}
            <Link to="/products" className="checkout-secondary-button">Tiếp tục mua sắm</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
