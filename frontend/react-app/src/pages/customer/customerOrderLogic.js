// Logic thuần của trang đơn hàng phía khách.

/** Bộ lọc trạng thái (trạng thái thật của hệ thống). */
export const ORDER_STATUS_FILTERS = [
  { value: "", label: "Tất cả đơn hàng" },
  { value: "PENDING", label: "Chờ xác nhận" },
  { value: "CONFIRMED", label: "Đã xác nhận" },
  { value: "HANDED_TO_SHIPPER", label: "Đã giao cho vận chuyển" },
  { value: "SHIPPING", label: "Đang giao" },
  { value: "COMPLETED", label: "Hoàn thành" },
  { value: "DELIVERY_FAILED", label: "Giao không thành công" },
  { value: "RETURNED", label: "Đã hoàn hàng" },
  { value: "CANCELLED", label: "Đã hủy" },
];

export const PAYMENT_METHOD_LABELS = { COD: "Thanh toán khi nhận hàng", VNPAY: "VNPay", MOMO: "Ví MoMo", CASH: "Tiền mặt" };

export const PAYMENT_STATUS_LABELS = {
  UNPAID: "Chưa thanh toán",
  AWAITING_PAYMENT: "Chờ thanh toán",
  PAID: "Đã thanh toán",
  PARTIALLY_PAID: "Đã thanh toán một phần",
  ABANDONED: "Không thanh toán",
};

export const PAYMENT_ATTEMPT_LABELS = { PENDING: "Đang chờ", SUCCESS: "Thành công", FAILED: "Không thành công", CANCELLED: "Đã hủy" };

/** Các bước chính của đơn giao hàng (dùng cho thanh tiến trình). */
export const ORDER_STEPS = ["PENDING", "CONFIRMED", "HANDED_TO_SHIPPER", "SHIPPING", "COMPLETED"];

/** Trạng thái kết thúc không theo luồng chính. */
export const OFF_TRACK = new Set(["CANCELLED", "DELIVERY_FAILED", "RETURNED"]);

/**
 * Tiến trình đơn: các bước chính kèm thời điểm đạt tới (lấy mốc đầu tiên trong lịch sử), bước hiện tại, và trạng thái
 * kết thúc đặc biệt (hủy / giao thất bại / hoàn hàng) nếu có.
 */
export function orderProgress(status, history = []) {
  const reachedAt = new Map();
  history.forEach((entry) => {
    if (!reachedAt.has(entry.status)) reachedAt.set(entry.status, entry.at);
  });
  const currentIndex = ORDER_STEPS.indexOf(status);
  const completed = status === "COMPLETED";
  return {
    steps: ORDER_STEPS.map((step, index) => {
      const current = index === currentIndex && !completed;
      return {
        status: step,
        at: reachedAt.get(step) || null,
        // Đã qua: trước bước hiện tại, hoặc đã có trong lịch sử (đơn hủy / giao thất bại giữ các bước đã đi qua).
        done: completed || (!current && (index < currentIndex || reachedAt.has(step))),
        current,
      };
    }),
    offTrack: OFF_TRACK.has(status) ? { status, at: [...history].reverse().find((h) => h.status === status)?.at || null } : null,
  };
}

/** Thao tác khách được làm với đơn. */
export function orderActions(order) {
  if (!order) return { cancel: false, pay: false, switchToCod: false, track: [] };
  return {
    cancel: Boolean(order.canCancel),
    pay: Boolean(order.canPay),
    switchToCod: Boolean(order.canSwitchToCod),
    track: (order.parts || []).filter((part) => part.trackingUrl).map((part) => ({ code: part.orderCode, url: part.trackingUrl, store: part.storeName })),
  };
}

/** Thời gian còn lại để thanh toán, dạng "12 phút". */
export function paymentCountdown(dueAt, now = Date.now()) {
  if (!dueAt) return null;
  const left = new Date(dueAt).getTime() - now;
  if (left <= 0) return "đã hết hạn";
  const minutes = Math.floor(left / 60000);
  const seconds = Math.floor((left % 60000) / 1000);
  return minutes > 0 ? `${minutes} phút ${String(seconds).padStart(2, "0")} giây` : `${seconds} giây`;
}

export const formatDateTime = (value) => (value ? new Date(value).toLocaleString("vi-VN", {
  hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit", year: "numeric",
}) : "");
