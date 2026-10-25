// Định dạng số liệu trang tổng quan quản trị.

const WEEKDAYS = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"];

export const money = value => `${Math.round(Number(value || 0)).toLocaleString("vi-VN")}đ`;

export const count = value => Number(value || 0).toLocaleString("vi-VN");

/** Tiền rút gọn cho thẻ số liệu: 950.000 → "950.000", 685.400.000 → "685,4 tr", 8.200.000.000 → "8,2 tỷ". */
export function compactMoney(value) {
  const amount = Number(value || 0);
  const short = (n, unit) => `${(Math.round(n * 10) / 10).toLocaleString("vi-VN")} ${unit}`;
  if (Math.abs(amount) >= 1e9) return short(amount / 1e9, "tỷ");
  if (Math.abs(amount) >= 1e6) return short(amount / 1e6, "tr");
  return Math.round(amount).toLocaleString("vi-VN");
}

/** Dòng so sánh với kỳ trước (mặc định cùng kỳ tháng trước); tone dùng chọn màu (up / down / flat). */
export function changeVsPrevious(current, previous, {
  suffix = "so với cùng kỳ tháng trước", emptyText = "Tháng trước chưa phát sinh",
} = {}) {
  const now = Number(current || 0);
  const before = Number(previous || 0);
  if (now === before) return { text: `Không đổi ${suffix}`, tone: "flat" };
  if (before === 0) return { text: emptyText, tone: "up" };
  const percent = Math.round(((now - before) / Math.abs(before)) * 1000) / 10;
  const arrow = percent > 0 ? "↑" : "↓";
  return {
    text: `${arrow} ${Math.abs(percent).toLocaleString("vi-VN")}% ${suffix}`,
    tone: percent > 0 ? "up" : "down",
  };
}

export const TONE_CLASSES = { up: "text-emerald-400", down: "text-red-400", flat: "text-zinc-400" };

/** "2026-10-25" → "T7 25/10". Đọc chuỗi ngày theo từng phần để không lệch múi giờ. */
export function dayLabel(isoDate) {
  const [year, month, day] = String(isoDate).split("-").map(Number);
  const weekday = WEEKDAYS[new Date(year, month - 1, day).getDay()];
  return `${weekday} ${String(day).padStart(2, "0")}/${String(month).padStart(2, "0")}`;
}

/** "2026-10-25" → "25 tháng 10, 2026". */
export function longDate(isoDate) {
  if (!isoDate) return "";
  const [year, month, day] = String(isoDate).split("-");
  return `${day} tháng ${month}, ${year}`;
}

export function greeting(hour) {
  if (hour < 11) return "Chào buổi sáng";
  if (hour < 14) return "Chào buổi trưa";
  if (hour < 18) return "Chào buổi chiều";
  return "Chào buổi tối";
}

/** Tóm tắt biểu đồ năm: doanh thu năm nay, tăng trưởng so với năm trước (null nếu năm trước bằng 0), năm cao nhất. */
export function yearSummary(points = []) {
  if (points.length === 0) return null;
  const current = points[points.length - 1];
  const previous = points.length > 1 ? Number(points[points.length - 2].revenue || 0) : 0;
  const currentRevenue = Number(current.revenue || 0);
  const best = points.reduce((top, point) => (Number(point.revenue || 0) > Number(top.revenue || 0) ? point : top), points[0]);
  return {
    year: current.label,
    revenue: currentRevenue,
    growth: previous === 0 ? null : Math.round(((currentRevenue - previous) / previous) * 1000) / 10,
    bestYear: Number(best.revenue || 0) > 0 ? best.label : null,
  };
}

/** Tên biến thể cho danh sách sắp hết hàng: "Áo sơ mi · Trắng / M". */
export function variantLabel(item) {
  const options = [item.color, item.size].filter(Boolean).join(" / ");
  return options ? `${item.productName} · ${options}` : item.productName;
}
