export const PROMOTION_TYPES = [
  { value: "ORDER", label: "Theo đơn hàng" },
  { value: "PRODUCT", label: "Theo sản phẩm" },
  { value: "POINT_REDEMPTION", label: "Đổi bằng điểm" },
];

export const PROMOTION_STATUSES = [
  { value: "SCHEDULED", label: "Sắp diễn ra", className: "bg-purple-500/20 text-purple-300" },
  { value: "ACTIVE", label: "Đang hoạt động", className: "bg-emerald-500/20 text-emerald-400" },
  { value: "EXPIRED", label: "Hết hạn", className: "bg-zinc-700 text-zinc-300" },
  { value: "INACTIVE", label: "Ngừng hoạt động", className: "bg-red-500/20 text-red-400" },
];

export const DISCOUNT_TYPES = [
  { value: "PERCENT", label: "Theo phần trăm" },
  { value: "FIXED", label: "Theo số tiền" },
];

export function labelOf(options, value) {
  return options.find((option) => option.value === value)?.label || value || "";
}

export function formatMoney(value) {
  return value === null || value === undefined || value === "" ? "" : `${Number(value).toLocaleString("vi-VN")}đ`;
}

export function formatDiscount(promotion) {
  if (!promotion) return "";
  if (promotion.discountType === "PERCENT") {
    const cap = promotion.maxDiscount ? ` (tối đa ${formatMoney(promotion.maxDiscount)})` : "";
    return `${Number(promotion.discountValue)}%${cap}`;
  }
  return formatMoney(promotion.discountValue);
}

/** LocalDateTime từ API ("2026-10-12T08:00:00") sang giá trị của input datetime-local. */
export function toInputDateTime(value) {
  return value ? String(value).slice(0, 16) : "";
}

export function formatDateTime(value) {
  if (!value) return "";
  const [date, time = ""] = String(value).split("T");
  const [year, month, day] = date.split("-");
  return `${day}/${month}/${year} ${time.slice(0, 5)}`.trim();
}

export function emptyPromotionForm() {
  return {
    name: "",
    promotionType: "ORDER",
    discountType: "PERCENT",
    discountValue: "",
    startDate: "",
    endDate: "",
    minOrderValue: "",
    maxDiscount: "",
    usageLimit: "",
    usagePerUser: "",
    pointsRequired: "",
    products: [],
    tierIds: [],
  };
}

export function promotionToForm(promotion) {
  return {
    ...emptyPromotionForm(),
    name: promotion.name || "",
    promotionType: promotion.promotionType,
    discountType: promotion.discountType,
    discountValue: promotion.discountValue ?? "",
    startDate: toInputDateTime(promotion.startDate),
    endDate: toInputDateTime(promotion.endDate),
    minOrderValue: promotion.minOrderValue ?? "",
    maxDiscount: promotion.maxDiscount ?? "",
    usageLimit: promotion.usageLimit ?? "",
    usagePerUser: promotion.usagePerUser ?? "",
    pointsRequired: promotion.pointsRequired ?? "",
    products: promotion.products || [],
    tierIds: (promotion.tiers || []).map((tier) => tier.id),
  };
}

const numberOrNull = (value) => (value === "" || value === null || value === undefined ? null : Number(value));
const withSeconds = (value) => (value && value.length === 16 ? `${value}:00` : value || null);

/** Chỉ gửi các trường thuộc loại khuyến mãi đang chọn. */
export function formToRequest(form) {
  const type = form.promotionType;
  const percent = form.discountType === "PERCENT";
  return {
    name: form.name.trim(),
    promotionType: type,
    discountType: form.discountType,
    discountValue: numberOrNull(form.discountValue),
    startDate: withSeconds(form.startDate),
    endDate: withSeconds(form.endDate),
    minOrderValue: type === "PRODUCT" ? null : numberOrNull(form.minOrderValue),
    maxDiscount: percent ? numberOrNull(form.maxDiscount) : null,
    usageLimit: numberOrNull(form.usageLimit),
    usagePerUser: type === "POINT_REDEMPTION" ? null : numberOrNull(form.usagePerUser),
    pointsRequired: type === "POINT_REDEMPTION" ? numberOrNull(form.pointsRequired) : null,
    productIds: type === "PRODUCT" ? form.products.map((product) => product.id) : [],
    tierIds: form.tierIds,
  };
}

/** Kiểm tra nhanh phía client; backend vẫn là nơi kiểm tra cuối cùng. */
export function validatePromotionForm(form) {
  if (!form.name.trim()) return "Vui lòng nhập tên khuyến mãi.";
  const value = Number(form.discountValue);
  if (form.discountValue === "" || !(value > 0)) return "Giá trị giảm phải lớn hơn 0.";
  if (form.discountType === "PERCENT" && value > 100) return "Phần trăm giảm không được vượt quá 100.";
  if (!form.startDate || !form.endDate) return "Vui lòng chọn ngày bắt đầu và ngày kết thúc.";
  if (form.endDate <= form.startDate) return "Ngày kết thúc phải sau ngày bắt đầu.";
  if (form.promotionType === "PRODUCT" && form.products.length === 0) return "Vui lòng chọn ít nhất một sản phẩm.";
  if (form.promotionType === "POINT_REDEMPTION" && !(Number(form.pointsRequired) > 0))
    return "Số điểm cần đổi phải lớn hơn 0.";
  return null;
}
