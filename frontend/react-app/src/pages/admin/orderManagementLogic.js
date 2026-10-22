// Nhãn, màu và tham số truy vấn của trang quản trị đơn hàng.

export const ORDER_STATUS_LABELS = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  HANDED_TO_SHIPPER: "Đã giao vận chuyển",
  SHIPPING: "Đang giao",
  COMPLETED: "Hoàn thành",
  DELIVERY_FAILED: "Giao thất bại",
  RETURNED: "Đã hoàn hàng",
  CANCELLED: "Đã hủy",
};

export const ORDER_STATUS_STYLES = {
  PENDING: "bg-amber-500/20 text-amber-300",
  CONFIRMED: "bg-blue-500/20 text-blue-300",
  HANDED_TO_SHIPPER: "bg-indigo-500/20 text-indigo-300",
  SHIPPING: "bg-sky-500/20 text-sky-300",
  COMPLETED: "bg-emerald-500/20 text-emerald-400",
  DELIVERY_FAILED: "bg-orange-500/20 text-orange-300",
  RETURNED: "bg-zinc-700 text-zinc-300",
  CANCELLED: "bg-red-500/20 text-red-400",
};

/** Trạng thái hiển thị ở bộ lọc theo loại đơn: đơn offline chỉ có Hoàn thành / Đã hủy / Đã hoàn hàng. */
export function statusOptions(type) {
  const offline = ["COMPLETED", "CANCELLED", "RETURNED"];
  return Object.keys(ORDER_STATUS_LABELS).filter(code => type === "ONLINE" || offline.includes(code));
}

export const PAYMENT_METHOD_LABELS = { CASH: "Tiền mặt", COD: "Thanh toán khi nhận", VNPAY: "VNPay", MOMO: "MoMo" };
export const PAYMENT_STATUS_LABELS = {
  PAID: "Đã thanh toán", PARTIALLY_PAID: "Đã thu một phần", UNPAID: "Chưa thanh toán", FAILED: "Thanh toán lỗi",
  REFUNDED: "Đã hoàn tiền", AWAITING_PAYMENT: "Chờ khách thanh toán", ABANDONED: "Không thanh toán",
};
/** Trạng thái của từng lần thanh toán (một đơn trả qua cổng có thể có nhiều lần). */
export const PAYMENT_ATTEMPT_LABELS = { PENDING: "Chờ khách thanh toán", SUCCESS: "Thành công", FAILED: "Thất bại", CANCELLED: "Đã hủy" };
export const ACTOR_LABELS = { USER: "Nhân viên", CUSTOMER: "Khách hàng", SYSTEM: "Hệ thống", SHIPPER: "Đơn vị vận chuyển" };

export const SHIPMENT_STATUS_LABELS = {
  ASSIGNING_DRIVER: "Đang tìm tài xế",
  DRIVER_ASSIGNED: "Tài xế đã nhận chuyến",
  PICKED_UP: "Đã lấy hàng, đang giao",
  COMPLETED: "Giao thành công",
  FAILED: "Không thành công",
  CANCELLED: "Đã hủy chuyến",
  REPLACED: "Hãng đã thay chuyến khác",
};

export const REFUND_STATUS_LABELS = { PENDING: "Chờ hoàn tiền", COMPLETED: "Đã hoàn tiền", FAILED: "Hoàn tiền lỗi" };

/** Nút giao vận chuyển: lần đầu hay gửi lại sau khi chuyến trước thất bại. */
export function handoverLabel(shipments = []) {
  return shipments.some(s => s.status === "FAILED") ? "Gửi lại yêu cầu giao hàng" : "Giao cho vận chuyển";
}

export const EMPTY_FILTERS = { keyword: "", status: "", fromDate: "", toDate: "", handlerId: "", paymentMethod: "" };

/** Tham số gửi API danh sách; trang trên giao diện bắt đầu từ 1, API bắt đầu từ 0. */
export function listParams({ storeId, type, hidden, filters, page, size, sort }) {
  return {
    storeId: storeId || undefined,
    type,
    hidden: hidden ? "true" : undefined,
    keyword: filters.keyword.trim() || undefined,
    status: filters.status || undefined,
    fromDate: filters.fromDate || undefined,
    toDate: filters.toDate || undefined,
    handlerId: filters.handlerId || undefined,
    paymentMethod: filters.paymentMethod || undefined,
    page: Math.max(0, page - 1),
    size,
    sort: `${sort.field},${sort.direction}`,
  };
}

/** Bấm lại cột đang sắp thì đảo chiều; cột mới bắt đầu giảm dần. */
export function nextSort(current, field) {
  if (current.field === field) return { field, direction: current.direction === "desc" ? "asc" : "desc" };
  return { field, direction: "desc" };
}

/** Tên hiển thị ở cột khách: khách thành viên, người nhận (online) hoặc khách vãng lai. */
export function customerLabel(row) {
  if (row.customerName) return { name: row.customerName, phone: row.customerPhone };
  if (row.receiverName) return { name: row.receiverName, phone: row.receiverPhone };
  return { name: "Khách vãng lai", phone: null };
}

export function validateDates(filters) {
  return filters.fromDate && filters.toDate && filters.fromDate > filters.toDate ? "Từ ngày phải trước hoặc bằng đến ngày." : "";
}
