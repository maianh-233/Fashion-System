// Nhãn, định dạng và xuất CSV cho trang báo cáo thống kê.
import { changeVsPrevious } from "./dashboardLogic.js";
import { PAYMENT_METHOD_LABELS } from "./orderManagementLogic.js";

export const PERIOD_OPTIONS = [
  ["LAST_7_DAYS", "7 ngày gần nhất"],
  ["LAST_30_DAYS", "30 ngày gần nhất"],
  ["THIS_QUARTER", "Quý này"],
  ["LAST_12_MONTHS", "12 tháng gần nhất"],
];

export const ORDER_TYPE_OPTIONS = [
  ["", "Tất cả đơn hàng"],
  ["ONLINE", "Đơn online"],
  ["OFFLINE", "Đơn tại cửa hàng"],
];

export const ORDER_TYPE_LABELS = { ONLINE: "Online", OFFLINE: "Tại cửa hàng" };

export const paymentLabel = key => PAYMENT_METHOD_LABELS[key] || (key === "OTHER" ? "Khác" : key);

/** Nhóm trạng thái cho biểu đồ tròn; nhóm không có đơn bị bỏ. */
const STATUS_GROUPS = [
  { name: "Hoàn thành", keys: ["COMPLETED"], color: "#f59e0b" },
  { name: "Đang xử lý", keys: ["PENDING", "CONFIRMED", "HANDED_TO_SHIPPER", "SHIPPING"], color: "#38bdf8" },
  { name: "Hoàn hàng / giao thất bại", keys: ["RETURNED", "DELIVERY_FAILED"], color: "#a1a1aa" },
  { name: "Đã hủy", keys: ["CANCELLED"], color: "#ef4444" },
];

export function statusGroups(counts = {}) {
  return STATUS_GROUPS
    .map(group => ({ name: group.name, color: group.color, value: group.keys.reduce((sum, key) => sum + Number(counts[key] || 0), 0) }))
    .filter(group => group.value > 0);
}

/** Tỷ lệ số đơn của từng dòng; {@code label} lấy theo hàm đặt tên. */
export function shares(rows = [], label = key => key) {
  const total = rows.reduce((sum, row) => sum + Number(row.orders || 0), 0);
  return rows.map(row => ({
    key: row.key,
    label: label(row.key),
    orders: Number(row.orders || 0),
    revenue: Number(row.revenue || 0),
    percent: total === 0 ? 0 : Math.round((Number(row.orders || 0) / total) * 1000) / 10,
  }));
}

/** Nhãn trục: ngày "2026-10-05" → "05/10", tháng "2026-10" → "10/2026". */
export function seriesLabel(label, granularity) {
  const [year, month, day] = String(label).split("-");
  return granularity === "MONTH" ? `${month}/${year}` : `${day}/${month}`;
}

/** "2026-05" → "T5/2026". */
export function monthLabel(value) {
  const [year, month] = String(value).split("-");
  return `T${Number(month)}/${year}`;
}

export const changeVsPreviousPeriod = (current, previous) =>
  changeVsPrevious(current, previous, { suffix: "so với kỳ trước", emptyText: "Kỳ trước chưa phát sinh" });

/** Chênh lệch tỷ lệ (điểm phần trăm) so với kỳ trước; null khi một trong hai kỳ chưa có khách. */
export function rateChange(rate, previousRate) {
  if (rate === null || rate === undefined || previousRate === null || previousRate === undefined) return null;
  const diff = Math.round((rate - previousRate) * 10) / 10;
  return { text: `${diff > 0 ? "+" : ""}${diff.toLocaleString("vi-VN")} điểm % so với kỳ trước`, tone: diff > 0 ? "up" : diff < 0 ? "down" : "flat" };
}

/** Khoảng ngày hiển thị: to là mốc loại trừ nên lùi một ngày. */
export function rangeLabel(from, to) {
  if (!from || !to) return "";
  const format = value => { const [y, m, d] = value.split("-"); return `${d}/${m}/${y}`; };
  const [y, m, d] = to.split("-").map(Number);
  const last = new Date(Date.UTC(y, m - 1, d - 1)).toISOString().slice(0, 10);
  return `${format(from)} – ${format(last)}`;
}

const BOM = String.fromCharCode(0xfeff);

function csvCell(value) {
  const text = value === null || value === undefined ? "" : String(value);
  return /[",\n\r;]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

/**
 * CSV nhiều phần cho Excel: mỗi phần {title, headers, rows}, cách nhau một dòng trống.
 * Có BOM để Excel nhận đúng tiếng Việt UTF-8.
 */
export function buildCsv(sections) {
  const lines = [];
  sections.forEach((section, index) => {
    if (index > 0) lines.push("");
    if (section.title) lines.push(csvCell(section.title));
    lines.push(section.headers.map(csvCell).join(","));
    section.rows.forEach(row => lines.push(row.map(csvCell).join(",")));
  });
  return BOM + lines.join("\r\n");
}

export function downloadCsv(filename, sections) {
  const blob = new Blob([buildCsv(sections)], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

/** Các bảng có nút xuất riêng; dùng chung cho xuất toàn bộ báo cáo. */
export const TABLES = {
  topProducts: report => ({
    title: "Top sản phẩm bán chạy",
    headers: ["Sản phẩm", "Đã bán", "Doanh thu (đ)", "Tồn kho"],
    rows: report.topProducts.map(row => [row.productName, row.sold, Number(row.revenue), row.stock]),
  }),
  lowStock: report => ({
    title: "Sản phẩm sắp hết hàng",
    headers: ["SKU", "Sản phẩm", "Màu", "Size", "Cửa hàng", "Tồn kho"],
    rows: report.lowStock.map(row => [row.sku, row.productName, row.color, row.size, row.storeName, row.quantity]),
  }),
  vipCustomers: report => ({
    title: "Top khách VIP",
    headers: ["Khách hàng", "Mã khách", "Hạng", "Số đơn", "Chi tiêu (đ)"],
    rows: report.vipCustomers.map(row => [row.fullName, row.customerCode, row.tier, row.orders, Number(row.spend)]),
  }),
};

export function reportSections(report) {
  const o = report.overview;
  return [
    {
      title: `Báo cáo thống kê ${rangeLabel(report.from, report.to)} · ${report.storeName || "Toàn hệ thống"}`
        + ` · ${ORDER_TYPE_OPTIONS.find(([value]) => value === (report.orderType || ""))?.[1]}`,
      headers: ["Chỉ số", "Kỳ này", "Kỳ trước"],
      rows: [
        ["Doanh thu (đ)", Number(o.revenue), Number(o.previousRevenue)],
        ["Số đơn", o.orders, o.previousOrders],
        ["Lợi nhuận gộp ước tính (đ)", Number(o.grossProfit), Number(o.previousGrossProfit)],
        ["Khách hàng mới", o.newCustomers, o.previousNewCustomers],
        ["Tỷ lệ khách quay lại (%)", report.retention.rate ?? "", report.retention.previousRate ?? ""],
      ],
    },
    {
      title: "Doanh thu theo thời gian",
      headers: [report.granularity === "MONTH" ? "Tháng" : "Ngày", "Doanh thu (đ)", "Số đơn"],
      rows: report.revenueSeries.map(point => [point.label, Number(point.revenue), point.orders]),
    },
    TABLES.topProducts(report),
    TABLES.lowStock(report),
    TABLES.vipCustomers(report),
    {
      title: "Kho",
      headers: ["Chỉ số", "Giá trị"],
      rows: [
        ["Nhập kho trong kỳ (sản phẩm)", report.inventory.importedQuantity],
        ["Xuất kho trong kỳ (sản phẩm)", report.inventory.exportedQuantity],
        ["Tồn hiện tại (sản phẩm)", report.inventory.stockQuantity],
        ["Giá trị tồn theo giá nhập (đ)", Number(report.inventory.stockValue)],
        ["Mã tồn chưa bán trong kỳ", report.inventory.unsoldVariants],
      ],
    },
  ];
}
