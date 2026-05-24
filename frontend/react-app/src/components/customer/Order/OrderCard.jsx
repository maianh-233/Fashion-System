import {
  Package,
  Calendar,
  CreditCard,
  Truck,
  MessageCircle,
  XCircle,
  Receipt,
} from "lucide-react";

import { getStatusColor, getStatusText } from "../../../hooks/orderHelpers";

const formatVND = (value) =>
  (Number(value ?? 0)).toLocaleString("vi-VN") + " ₫";

export default function OrderCard({ order, onChat, onCancel }) {
  if (!order) return null;

  return (
    <div className="
      bg-zinc-900 border border-zinc-800 rounded-2xl md:rounded-3xl
      p-4 md:p-6
      hover:border-amber-400 transition
      space-y-4
    ">

      {/* ================= HEADER ================= */}
      <div className="flex flex-col md:flex-row md:justify-between md:items-start gap-3">

        {/* LEFT */}
        <div>
          <div className="flex items-center gap-2 text-base md:text-lg font-semibold">
            <Package size={18} className="text-amber-400" />
            <span className="truncate">
              #{order.order_code || "N/A"}
            </span>
          </div>

          <div className="flex items-center gap-2 text-xs md:text-sm text-zinc-400 mt-1">
            <Calendar size={14} />
            <span>
              {order.created_at
                ? new Date(order.created_at).toLocaleString("vi-VN")
                : "Không có ngày"}
            </span>
          </div>
        </div>

        {/* RIGHT STATUS */}
        <div className="flex flex-col md:items-end gap-2">

          <span className={`${getStatusColor(order.status)} font-medium text-sm`}>
            {getStatusText(order.status)}
          </span>

          <span className={`
            text-xs px-3 py-1 rounded-full inline-flex items-center gap-1 w-fit
            ${
              order.payment_status === "PAID"
                ? "bg-emerald-500/10 text-emerald-400"
                : "bg-yellow-500/10 text-yellow-400"
            }
          `}>
            <CreditCard size={12} />
            {order.payment_status === "PAID"
              ? "Đã thanh toán"
              : "Chưa thanh toán"}
          </span>
        </div>
      </div>

      {/* ================= INFO ================= */}
      <div className="
        grid grid-cols-2 md:grid-cols-4
        gap-3 md:gap-4
        text-xs md:text-sm text-zinc-400
      ">

        <div className="flex items-start gap-2">
          <Receipt size={16} className="text-zinc-500 mt-0.5" />
          <div>
            <p>Loại đơn</p>
            <p className="text-zinc-200 font-medium">
              {order.order_type || "N/A"}
            </p>
          </div>
        </div>

        <div>
          <p>Tạm tính</p>
          <p className="text-zinc-200">
            {formatVND(order.subtotal)}
          </p>
        </div>

        <div>
          <p>Giảm giá</p>
          <p className="text-zinc-200">
            -{formatVND(order.discount_total)}
          </p>
        </div>

        <div className="flex items-start gap-2">
          <Truck size={16} className="text-zinc-500 mt-0.5" />
          <div>
            <p>Phí ship</p>
            <p className="text-zinc-200">
              {formatVND(order.shipping_fee)}
            </p>
          </div>
        </div>
      </div>

      {/* ================= NOTE ================= */}
      {order.note && (
        <div className="
          text-xs md:text-sm text-zinc-400 italic
          border-l-2 border-zinc-700 pl-3
        ">
          “{order.note}”
        </div>
      )}

      {/* ================= FOOTER ================= */}
      <div className="
        flex flex-col md:flex-row
        md:justify-between md:items-end
        gap-4 pt-3 border-t border-zinc-800
      ">

        {/* TOTAL */}
        <div>
          <p className="text-xs md:text-sm text-zinc-400 flex items-center gap-2">
            <CreditCard size={14} />
            Tổng thanh toán
          </p>

          <p className="text-xl md:text-2xl font-semibold text-amber-400">
            {formatVND(order.total_amount)}
          </p>
        </div>

        {/* ACTIONS */}
        <div className="flex gap-3 w-full md:w-auto">

          <button
            onClick={() => onChat?.(order.id)}
            className="
              flex-1 md:flex-none
              flex items-center justify-center gap-2
              bg-emerald-500/10 hover:bg-emerald-500/20
              text-emerald-400
              px-4 md:px-5 py-3
              rounded-xl md:rounded-2xl
              text-sm
            "
          >
            <MessageCircle size={16} />
            Chat
          </button>

          {order.status === "PENDING" && (
            <button
              onClick={() => onCancel?.(order.id)}
              className="
                flex-1 md:flex-none
                flex items-center justify-center gap-2
                border border-red-500/30
                text-red-400
                hover:bg-red-500/10
                px-4 md:px-5 py-3
                rounded-xl md:rounded-2xl
                text-sm
              "
            >
              <XCircle size={16} />
              Hủy
            </button>
          )}
        </div>
      </div>
    </div>
  );
}