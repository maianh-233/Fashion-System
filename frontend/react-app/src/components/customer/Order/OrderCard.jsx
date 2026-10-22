import { Link } from "react-router-dom";
import { Calendar, CreditCard, Eye, MessageCircle, Package, Store, Wallet, XCircle } from "lucide-react";
import Button from "../../common/Button";
import { formatPrice } from "../../../pages/customer/storefrontLogic";
import {
  PAYMENT_METHOD_LABELS, PAYMENT_STATUS_LABELS, formatDateTime, paymentCountdown,
} from "../../../pages/customer/customerOrderLogic";

/** Thẻ đơn trong "Đơn hàng của tôi" (dữ liệu API). */
export default function OrderCard({ order, busy, onChat, onCancel, onPay }) {
  if (!order) return null;
  const items = order.parts.flatMap((part) => part.items);
  const preview = items.slice(0, 2).map((item) => `${item.productName} × ${item.quantity}`).join(", ");
  const more = items.length > 2 ? ` và ${items.length - 2} sản phẩm khác` : "";
  const stores = [...new Set(order.parts.map((part) => part.storeName).filter(Boolean))];

  return (
    <article className="customer-card customer-order-card order-card">
      <div className="order-card__header">
        <div>
          <p className="customer-card__eyebrow">Đơn hàng</p>
          <div className="order-card__code">
            <Package size={18} className="text-amber-400" />
            <span className="truncate">#{order.orderCode}</span>
          </div>
          <div className="order-card__date">
            <Calendar size={14} />
            <span>{formatDateTime(order.createdAt)}</span>
          </div>
        </div>

        <div className="order-card__statuses">
          <span className="customer-card__status">{order.statusLabel}</span>
          <span className="order-card__payment">
            <CreditCard size={12} />
            {PAYMENT_STATUS_LABELS[order.paymentStatus] || order.paymentStatus}
          </span>
        </div>
      </div>

      <div className="order-card__info">
        <div>
          <Package size={15} />
          <div>
            <p>{order.itemCount} sản phẩm</p>
            <strong>{preview}{more}</strong>
          </div>
        </div>
        <div>
          <Store size={15} />
          <div>
            <p>{order.split ? `Giao thành ${order.parts.length} phần` : "Cửa hàng"}</p>
            <strong>{stores.join(", ") || "Đang phân cửa hàng"}</strong>
          </div>
        </div>
        <div>
          <Wallet size={15} />
          <div>
            <p>Thanh toán</p>
            <strong>{PAYMENT_METHOD_LABELS[order.paymentMethod] || order.paymentMethod}</strong>
          </div>
        </div>
      </div>

      {order.canPay && order.paymentDueAt && (
        <div className="order-card__note">Vui lòng thanh toán trong {paymentCountdown(order.paymentDueAt)}, quá hạn đơn sẽ tự hủy.</div>
      )}
      {order.cancelReason && <div className="order-card__note">Lý do hủy: {order.cancelReason}</div>}

      <div className="order-card__footer">
        <div>
          <p className="order-card__total-label"><CreditCard size={14} /> Tổng thanh toán</p>
          <p className="order-card__total">{formatPrice(order.total)}</p>
        </div>

        <div className="order-card__actions">
          <Link to={`/orderdetail?id=${order.id}`} className="order-card__action"><Eye size={16} /> Chi tiết</Link>
          <Button onClick={() => onChat?.(order)} variant="unstyled" className="order-card__action">
            <MessageCircle size={16} /> Hỗ trợ
          </Button>
          {order.canPay && (
            <Button onClick={() => onPay?.(order)} variant="unstyled" className="order-card__action" loading={busy === "pay"}>
              <CreditCard size={16} /> Thanh toán
            </Button>
          )}
          {order.canCancel && (
            <Button onClick={() => onCancel?.(order)} variant="unstyled" className="order-card__action order-card__action--danger">
              <XCircle size={16} /> Hủy
            </Button>
          )}
        </div>
      </div>
    </article>
  );
}
