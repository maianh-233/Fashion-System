import Button from "../../common/Button";
import {
  Package,
  Calendar,
  CreditCard,
  Truck,
  MessageCircle,
  XCircle,
  Receipt,
} from "lucide-react";

import { getStatusText } from "../../../utils/orderHelpers";

const formatVND = (value) =>
  (Number(value ?? 0)).toLocaleString("vi-VN") + " ₫";

export default function OrderCard({ order, onChat, onCancel }) {
  if (!order) return null;

  return (
    <article className="customer-card customer-order-card order-card">

      {/* ================= HEADER ================= */}
      <div className="order-card__header">

        {/* LEFT */}
        <div>
          <p className="customer-card__eyebrow">Đơn hàng</p>
          <div className="order-card__code">
            <Package size={18} className="text-amber-400" />
            <span className="truncate">
              #{order.order_code || "N/A"}
            </span>
          </div>

          <div className="order-card__date">
            <Calendar size={14} />
            <span>
              {order.created_at
                ? new Date(order.created_at).toLocaleString("vi-VN")
                : "Không có ngày"}
            </span>
          </div>
        </div>

        {/* RIGHT STATUS */}
        <div className="order-card__statuses">

          <span className="customer-card__status">
            {getStatusText(order.status)}
          </span>

          <span className="order-card__payment">
            <CreditCard size={12} />
            {order.payment_status === "PAID"
              ? "Đã thanh toán"
              : "Chưa thanh toán"}
          </span>
        </div>
      </div>

      {/* ================= INFO ================= */}
      <div className="order-card__info">

        <div>
          <Receipt size={15} />
          <div>
            <p>Loại đơn</p>
            <strong>
              {order.order_type || "N/A"}
            </strong>
          </div>
        </div>

        <div>
          <p>Tạm tính</p>
          <strong>
            {formatVND(order.subtotal)}
          </strong>
        </div>

        <div>
          <p>Giảm giá</p>
          <strong>
            -{formatVND(order.discount_total)}
          </strong>
        </div>

        <div>
          <Truck size={15} />
          <div>
            <p>Phí ship</p>
            <strong>
              {formatVND(order.shipping_fee)}
            </strong>
          </div>
        </div>
      </div>

      {/* ================= NOTE ================= */}
      {order.note && (
        <div className="order-card__note">
          “{order.note}”
        </div>
      )}

      {/* ================= FOOTER ================= */}
      <div className="order-card__footer">

        {/* TOTAL */}
        <div>
          <p className="order-card__total-label">
            <CreditCard size={14} />
            Tổng thanh toán
          </p>

          <p className="order-card__total">
            {formatVND(order.total_amount)}
          </p>
        </div>

        {/* ACTIONS */}
        <div className="order-card__actions">

          <Button
            onClick={() => onChat?.(order.id)}
            variant="unstyled"
            className="order-card__action"
          >
            <MessageCircle size={16} />
            Chat
          </Button>

          {order.status === "PENDING" && (
            <Button
              onClick={() => onCancel?.(order.id)}
              variant="unstyled"
              className="order-card__action order-card__action--danger"
            >
              <XCircle size={16} />
              Hủy
            </Button>
          )}
        </div>
      </div>
    </article>
  );
}
