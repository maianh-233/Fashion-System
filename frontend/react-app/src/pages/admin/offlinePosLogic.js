// Logic thuần của màn hình bán tại quầy: giỏ hàng chỉ giữ biến thể + số lượng; mọi số tiền lấy từ backend.

export const MAX_QUANTITY = 9999;

/** Thêm biến thể vào giỏ; quét/nhập lại cùng mã thì cộng dồn số lượng (không vượt tồn). */
export function addToCart(cart, variant, quantity = 1) {
  const stock = Number(variant.stock ?? MAX_QUANTITY);
  const existing = cart.find(line => line.variantId === variant.variantId);
  if (existing) {
    const next = Math.min(existing.quantity + quantity, stock, MAX_QUANTITY);
    return { cart: cart.map(line => line.variantId === variant.variantId ? { ...line, quantity: next, stock } : line), capped: next < existing.quantity + quantity };
  }
  const first = Math.min(quantity, stock, MAX_QUANTITY);
  return { cart: [...cart, { variantId: variant.variantId, quantity: first, stock, preview: preview(variant) }], capped: first < quantity };
}

/** Thông tin hiển thị tạm trước khi quote trả về (tên, SKU, ảnh). */
function preview(variant) {
  return {
    productName: variant.productName, sku: variant.sku, color: variant.color, size: variant.size,
    imageUrl: variant.imageUrl, unitPrice: variant.salePrice ?? variant.price,
  };
}

export function setQuantity(cart, variantId, value) {
  const quantity = Math.floor(Number(value));
  if (!Number.isFinite(quantity) || quantity < 1) return cart;
  return cart.map(line => line.variantId === variantId
    ? { ...line, quantity: Math.min(quantity, line.stock ?? MAX_QUANTITY, MAX_QUANTITY) } : line);
}

export function removeLine(cart, variantId) {
  return cart.filter(line => line.variantId !== variantId);
}

/** Request gửi backend cho quote / voucher / tạo đơn. */
export function cartRequest({ storeId, customer, cart, promotionCodes, cashReceived, note }) {
  const cash = cashReceived === "" || cashReceived === null || cashReceived === undefined ? null : Number(cashReceived);
  return {
    storeId: storeId || null,
    customerId: customer?.id || null,
    lines: cart.map(({ variantId, quantity }) => ({ variantId, quantity })),
    promotionCodes,
    cashReceived: Number.isFinite(cash) ? cash : null,
    note: note?.trim() || null,
  };
}

/**
 * Chọn/bỏ voucher theo quy tắc cộng dồn: 1 voucher toàn đơn (chọn cái mới thay cái cũ)
 * và nhiều khuyến mãi sản phẩm.
 */
export function toggleVoucher(selected, voucher, vouchers) {
  if (selected.includes(voucher.code)) return selected.filter(code => code !== voucher.code);
  if (voucher.type === "PRODUCT") return [...selected, voucher.code];
  const orderLevel = new Set(vouchers.filter(v => v.type !== "PRODUCT").map(v => v.code));
  return [...selected.filter(code => !orderLevel.has(code)), voucher.code];
}

/** Mệnh giá gợi ý cho tiền khách đưa: đúng số tiền và các mức làm tròn lên phổ biến. */
export function cashSuggestions(total) {
  const amount = Math.max(0, Math.ceil(Number(total) || 0));
  if (amount === 0) return [];
  const steps = [10_000, 50_000, 100_000, 500_000];
  const values = new Set([amount, ...steps.map(step => Math.ceil(amount / step) * step)]);
  return [...values].filter(value => value >= amount).sort((a, b) => a - b).slice(0, 4);
}

export const money = value => `${Math.round(Number(value || 0)).toLocaleString("vi-VN")}đ`;

export function voucherValueLabel(voucher) {
  if (voucher.discountType === "PERCENT") {
    const cap = voucher.maxDiscount ? ` (tối đa ${money(voucher.maxDiscount)})` : "";
    return `Giảm ${Number(voucher.discountValue)}%${cap}${voucher.type === "PRODUCT" ? " mỗi sản phẩm" : ""}`;
  }
  return `Giảm ${money(voucher.discountValue)}${voucher.type === "PRODUCT" ? " mỗi sản phẩm" : ""}`;
}
