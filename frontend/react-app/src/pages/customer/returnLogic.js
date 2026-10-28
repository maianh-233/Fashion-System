/** Trạng thái yêu cầu đổi / trả → nhãn màu. */
export const RETURN_STATUS_TONES = {
  REQUESTED: "pending", APPROVED: "info", COMPLETED: "success", REJECTED: "danger", CANCELLED: "muted",
};

export const RETURN_TYPE_LABELS = { RETURN: "Trả hàng", EXCHANGE: "Đổi size / màu" };

/** Lựa chọn ban đầu: mỗi dòng {checked, quantity, exchangeVariantId}. */
export function initialSelection(items = []) {
  return Object.fromEntries(items.map((item) => [item.orderItemId, { checked: false, quantity: 1, exchangeVariantId: "" }]));
}

/** Tiền dự kiến hoàn (trả hàng) theo giá thực trả của từng món. */
export function estimateRefund(items = [], selection = {}) {
  return items.reduce((sum, item) => {
    const chosen = selection[item.orderItemId];
    return chosen?.checked ? sum + Number(item.unitPrice) * chosen.quantity : sum;
  }, 0);
}

/** Lỗi của form (rỗng nếu hợp lệ). */
export function validateReturnForm({ type, reasonCode, items = [], selection = {} }) {
  const chosen = items.filter((item) => selection[item.orderItemId]?.checked);
  if (chosen.length === 0) return "Vui lòng chọn ít nhất một sản phẩm.";
  if (!reasonCode) return "Vui lòng chọn lý do.";
  for (const item of chosen) {
    const line = selection[item.orderItemId];
    if (!Number.isInteger(line.quantity) || line.quantity < 1 || line.quantity > item.requestable)
      return `Số lượng của ${item.productName} phải từ 1 đến ${item.requestable}.`;
    if (type === "EXCHANGE" && !line.exchangeVariantId) return `Chọn phân loại muốn đổi cho ${item.productName}.`;
  }
  return "";
}

/** Payload cho returnApi.create. */
export function buildReturnPayload({ orderId, type, reasonCode, note, items = [], selection = {} }) {
  return {
    orderId,
    type,
    reasonCode,
    note: note?.trim() || null,
    items: items.filter((item) => selection[item.orderItemId]?.checked).map((item) => ({
      orderItemId: item.orderItemId,
      quantity: selection[item.orderItemId].quantity,
      exchangeVariantId: type === "EXCHANGE" ? selection[item.orderItemId].exchangeVariantId : null,
    })),
  };
}

export const variantLabel = (color, size) => [color, size].filter((part) => part && String(part).trim()).join(" / ");
