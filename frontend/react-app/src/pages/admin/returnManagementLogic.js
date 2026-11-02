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

// ------------------------------------------------------------ báo cáo đổi trả theo size

export const REPORT_PERIODS = [
  { days: 30, label: "30 ngày" },
  { days: 90, label: "90 ngày" },
  { days: 180, label: "6 tháng" },
  { days: 365, label: "12 tháng" },
];

const isoDate = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;

/** Khoảng ngày (gồm cả hai đầu, giờ máy) của {@code days} ngày gần nhất tính tới hôm nay. */
export function reportRange(days, today = new Date()) {
  const from = new Date(today.getFullYear(), today.getMonth(), today.getDate() - (days - 1));
  return { from: isoDate(from), to: isoDate(today) };
}

export const FIT_HINT_LABELS = {
  RUNS_SMALL: "Form nhỏ · khách hay đổi lên size lớn hơn",
  RUNS_LARGE: "Form rộng · khách hay đổi xuống size nhỏ hơn",
};

export const formatRate = (value) => `${Number(value || 0).toLocaleString("vi-VN", { maximumFractionDigits: 1 })}%`;

/** Màu cho tỷ lệ "không vừa size": từ 10% là đỏ (ngưỡng cần xem lại bảng size), từ 5% là vàng. */
export function rateTone(rate) {
  const value = Number(rate || 0);
  if (value >= 10) return "text-rose-300";
  if (value >= 5) return "text-amber-300";
  return "text-zinc-300";
}

/** Bảng CSV: mỗi dòng là một size của một sản phẩm có đổi trả. */
export function sizeReportCsv(report) {
  const header = ["Sản phẩm", "Danh mục", "Size", "Đã bán", "Đổi/trả", "Tỷ lệ đổi/trả (%)", "Không vừa size", "Tỷ lệ không vừa (%)", "Đổi sang"];
  const rows = (report?.products || []).flatMap((product) => product.sizes.map((size) => [
    product.productName || "", product.categoryName || "", size.size, size.soldQuantity, size.returnedQuantity,
    size.returnRate, size.wrongSizeQuantity, size.wrongSizeRate,
    size.exchanges.map((flow) => `${flow.toSize} ×${flow.quantity}`).join(", "),
  ]));
  const cell = (value) => {
    const text = String(value ?? "");
    return /[",\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text;
  };
  return "\uFEFF" + [header, ...rows].map((row) => row.map(cell).join(",")).join("\n");
}
