export const RETURN_STATUS_OPTIONS = [
  { value: "", label: "Tất cả" },
  { value: "REQUESTED", label: "Chờ duyệt" },
  { value: "APPROVED", label: "Đã duyệt · chờ nhận hàng" },
  { value: "COMPLETED", label: "Hoàn tất" },
  { value: "REJECTED", label: "Từ chối" },
  { value: "CANCELLED", label: "Khách đã hủy" },
];

export const RETURN_STATUS_STYLES = {
  REQUESTED: "border-amber-400/30 bg-amber-500/10 text-amber-200",
  APPROVED: "border-sky-400/30 bg-sky-500/10 text-sky-200",
  COMPLETED: "border-emerald-400/30 bg-emerald-500/10 text-emerald-200",
  REJECTED: "border-rose-400/30 bg-rose-500/10 text-rose-200",
  CANCELLED: "border-zinc-600 bg-zinc-800 text-zinc-400",
};

/** Mặc định mọi món "còn tốt"; nhân viên đổi thành "lỗi" khi kiểm tra. */
export function defaultConditions(items = []) {
  return Object.fromEntries(items.map((item) => [item.id, item.condition || "GOOD"]));
}

/** Thao tác nhân viên được làm với yêu cầu ở trạng thái hiện tại. */
export function availableActions(request) {
  if (!request) return [];
  if (request.status === "REQUESTED") return ["approve", "reject"];
  if (request.status === "APPROVED") return ["complete", "reject"];
  if (request.status === "COMPLETED" && request.refundStatus === "PENDING") return ["confirmRefund"];
  return [];
}

export function normalizeReturnPage(response) {
  const meta = response?.page ?? response ?? {};
  return {
    rows: Array.isArray(response?.content) ? response.content : [],
    totalPages: Math.max(1, Number(meta.totalPages || 1)),
    totalElements: Number(meta.totalElements || 0),
  };
}

export const formatMoney = (value) =>
  value == null ? "" : `${Number(value).toLocaleString("vi-VN")} ₫`;
