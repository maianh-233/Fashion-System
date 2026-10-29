export const FLASH_STATUS = {
  UPCOMING: { label: "Sắp diễn ra", style: "border-sky-400/30 bg-sky-500/10 text-sky-200" },
  RUNNING: { label: "Đang chạy", style: "border-rose-400/30 bg-rose-500/10 text-rose-200" },
  ENDED: { label: "Đã kết thúc", style: "border-zinc-600 bg-zinc-800 text-zinc-400" },
  STOPPED: { label: "Đã dừng", style: "border-amber-400/30 bg-amber-500/10 text-amber-200" },
};

/** "yyyy-MM-ddTHH:mm" theo giờ máy cho ô datetime-local. */
export function toInputTime(date) {
  const d = new Date(date);
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** Form trống: bắt đầu sau 1 giờ (tròn giờ), kéo dài 2 giờ. */
export function emptyForm(now = new Date()) {
  const start = new Date(now);
  start.setMinutes(0, 0, 0);
  start.setHours(start.getHours() + 1);
  return { name: "", startsAt: toInputTime(start), endsAt: toInputTime(new Date(start.getTime() + 2 * 3600 * 1000)), items: [] };
}

export function formFromSale(sale) {
  return {
    name: sale.name,
    startsAt: toInputTime(sale.startsAt),
    endsAt: toInputTime(sale.endsAt),
    items: sale.items.map((item) => ({
      variantId: item.variantId,
      label: `${item.productName} · ${item.sku} (${[item.color, item.size].filter(Boolean).join(" / ")})`,
      regularPrice: item.regularPrice, salePrice: String(Number(item.salePrice)), quota: String(item.quota), sold: item.sold,
    })),
  };
}

export function validateSaleForm(form) {
  if (!form.name.trim()) return "Vui lòng nhập tên chương trình.";
  if (!form.startsAt || !form.endsAt || form.endsAt <= form.startsAt) return "Giờ kết thúc phải sau giờ bắt đầu.";
  if (form.items.length === 0) return "Thêm ít nhất một biến thể.";
  for (const item of form.items) {
    const price = Number(item.salePrice);
    const quota = Number(item.quota);
    if (!(price > 0) || price >= Number(item.regularPrice)) return `Giá flash của ${item.label} phải lớn hơn 0 và thấp hơn giá thường.`;
    if (!Number.isInteger(quota) || quota < Math.max(1, item.sold || 0)) return `Số suất của ${item.label} phải từ ${Math.max(1, item.sold || 0)} trở lên.`;
  }
  return "";
}

export function salePayload(form) {
  return {
    name: form.name.trim(),
    startsAt: `${form.startsAt}:00`,
    endsAt: `${form.endsAt}:00`,
    items: form.items.map((item) => ({ variantId: item.variantId, salePrice: Number(item.salePrice), quota: Number(item.quota) })),
  };
}
