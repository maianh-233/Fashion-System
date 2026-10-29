export const LOW_STOCK_TARGET = 10;

export const TRANSFER_STATUS = {
  REQUESTED: { label: "Chờ nơi gửi duyệt", style: "border-amber-400/30 bg-amber-500/10 text-amber-200" },
  SHIPPED: { label: "Đang chuyển", style: "border-sky-400/30 bg-sky-500/10 text-sky-200" },
  RECEIVED: { label: "Đã nhận", style: "border-emerald-400/30 bg-emerald-500/10 text-emerald-200" },
  REJECTED: { label: "Bị từ chối", style: "border-rose-400/30 bg-rose-500/10 text-rose-200" },
  CANCELLED: { label: "Đã hủy", style: "border-zinc-600 bg-zinc-800 text-zinc-400" },
};

/**
 * Số đề xuất xin thêm: đưa tổng tồn về {@link LOW_STOCK_TARGET}, trừ phần đang chờ / đang chuyển tới,
 * không vượt phần dư của cửa hàng gợi ý (giữ lại 5 món ở nơi gửi).
 */
export function suggestedQuantity(row, keepAtSource = 5) {
  const need = LOW_STOCK_TARGET - Number(row?.total || 0) - Number(row?.incoming || 0);
  const spare = Number(row?.suggestedStock || 0) - keepAtSource;
  return Math.max(0, Math.min(need, spare));
}

/** Gom các dòng đã chọn theo cửa hàng gửi được gợi ý → mỗi nhóm thành một yêu cầu điều chuyển. */
export function groupRequests(rows = [], picked = {}) {
  const groups = new Map();
  rows.forEach((row) => {
    const quantity = Number(picked[row.variantId]);
    if (!row.suggestedStore || !Number.isInteger(quantity) || quantity < 1) return;
    const key = row.suggestedStore.id;
    if (!groups.has(key)) groups.set(key, { fromStoreId: key, fromStoreName: row.suggestedStore.name, items: [] });
    groups.get(key).items.push({ variantId: row.variantId, quantity });
  });
  return [...groups.values()];
}

/** Phía người dùng được thao tác trên phiếu: from (nơi gửi), to (nơi nhận). */
export function transferActions(transfer, { isGlobal, currentStoreId }) {
  if (!transfer) return [];
  const atFrom = isGlobal || currentStoreId === transfer.fromStore?.id;
  const atTo = isGlobal || currentStoreId === transfer.toStore?.id;
  if (transfer.status === "REQUESTED") return [...(atFrom ? ["ship", "reject"] : []), ...(atTo ? ["cancel"] : [])];
  if (transfer.status === "SHIPPED" && atTo) return ["receive"];
  return [];
}

export const variantText = (item) => [item?.color, item?.size].filter(Boolean).join(" / ");
