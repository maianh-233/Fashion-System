export const STOCKTAKE_STATUS = {
  IN_PROGRESS: { label: "Đang đếm", tone: "sky" },
  SUBMITTED: { label: "Chờ duyệt", tone: "amber" },
  APPROVED: { label: "Đã duyệt", tone: "emerald" },
  CANCELLED: { label: "Đã hủy", tone: "zinc" },
};

export const ITEM_FILTERS = [
  { value: "ALL", label: "Tất cả" },
  { value: "UNCOUNTED", label: "Chưa đếm" },
  { value: "COUNTED", label: "Đã đếm" },
  { value: "VARIANCE", label: "Lệch", needsSystem: true },
  { value: "ADDED", label: "Phát sinh khi đếm" },
];

export const COST_SOURCE = { RECEIPT: "Giá nhập gần nhất", PRICE: "Giá bán (chưa có giá nhập)", NONE: "Chưa có giá" };

const vnd = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 });
const num = new Intl.NumberFormat("vi-VN");

export const money = (value) => (value == null ? "—" : vnd.format(Number(value)));
export const qty = (value) => (value == null ? "—" : num.format(value));
export const signed = (value) => (value == null ? "—" : value > 0 ? `+${num.format(value)}` : num.format(value));
export const percent = (value) => (value == null ? "—" : `${Number(value).toLocaleString("vi-VN", { maximumFractionDigits: 2 })}%`);

export const formatDateTime = (value) => (value ? new Date(value).toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "short" }) : "");

export const variantText = (item) => [item.color, item.size].filter(Boolean).join(" / ");

/** Chênh lệch → màu chữ (thiếu đỏ, thừa xanh). */
export const varianceTone = (value) => (value == null || value === 0 ? "text-zinc-300" : value < 0 ? "text-rose-300" : "text-emerald-300");
