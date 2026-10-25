// Kiểm tra và chuẩn hóa form hạng khách hàng.

export const DEFAULT_TIER = "REGULAR";

export const EMPTY_TIER = { code: "", name: "", minTotalSpent: "", discountPercent: "0" };

export function tierForm(tier) {
  return tier ? {
    code: tier.code || "",
    name: tier.name || "",
    minTotalSpent: String(Number(tier.minTotalSpent ?? 0)),
    discountPercent: String(Number(tier.discountPercent ?? 0)),
  } : { ...EMPTY_TIER };
}

/** Trả về thông báo lỗi đầu tiên, hoặc "" khi hợp lệ. {@code tiers} để kiểm tra trùng mã và trùng ngưỡng. */
export function validateTier(form, tiers = [], editingId = null) {
  const code = form.code.trim().toUpperCase();
  if (!code) return "Vui lòng nhập mã hạng.";
  if (!/^[A-Z0-9_]{1,50}$/.test(code)) return "Mã hạng chỉ gồm chữ không dấu, số và dấu gạch dưới (tối đa 50 ký tự).";
  if (!form.name.trim()) return "Vui lòng nhập tên hạng.";
  const minSpent = Number(form.minTotalSpent);
  if (form.minTotalSpent === "" || !Number.isFinite(minSpent) || minSpent < 0) return "Ngưỡng chi tiêu phải là số không âm.";
  const discount = Number(form.discountPercent || 0);
  if (!Number.isFinite(discount) || discount < 0 || discount > 100) return "Ưu đãi phải nằm trong khoảng 0–100%.";
  const others = tiers.filter(tier => tier.id !== editingId);
  if (others.some(tier => tier.code === code)) return "Mã hạng đã tồn tại.";
  if (others.some(tier => Number(tier.minTotalSpent) === minSpent)) return "Đã có hạng khác dùng ngưỡng chi tiêu này.";
  const editing = tiers.find(tier => tier.id === editingId);
  if (editing?.code === DEFAULT_TIER && (code !== DEFAULT_TIER || minSpent !== 0)) {
    return `Hạng mặc định ${DEFAULT_TIER} phải giữ mã và ngưỡng chi tiêu 0.`;
  }
  return "";
}

export function tierPayload(form) {
  return {
    code: form.code.trim().toUpperCase(),
    name: form.name.trim(),
    minTotalSpent: Number(form.minTotalSpent),
    discountPercent: Number(form.discountPercent || 0),
  };
}

/** Khoảng chi tiêu của từng hạng theo thứ tự tăng dần: "Từ 5.000.000đ đến dưới 20.000.000đ". */
export function spendRange(tiers, index) {
  const money = value => `${Math.round(Number(value || 0)).toLocaleString("vi-VN")}đ`;
  const from = tiers[index].minTotalSpent;
  const next = tiers[index + 1];
  return next ? `Từ ${money(from)} đến dưới ${money(next.minTotalSpent)}` : `Từ ${money(from)} trở lên`;
}
