// Logic thuần của danh sách yêu thích (lưu theo sản phẩm).

export const MAX_WISHLIST = 200;
export const WISHLIST_PAGE_SIZE = 24;

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** Danh sách đọc từ trình duyệt: chỉ giữ id hợp lệ, bỏ trùng, tối đa {@link MAX_WISHLIST}. */
export function sanitizeIds(value) {
  if (!Array.isArray(value)) return [];
  return [...new Set(value.filter((id) => typeof id === "string" && UUID_PATTERN.test(id)))].slice(0, MAX_WISHLIST);
}

/** Bật/tắt yêu thích; sản phẩm mới lên đầu. Ném lỗi khi đã đủ giới hạn. */
export function toggleId(ids, productId) {
  if (ids.includes(productId)) return ids.filter((id) => id !== productId);
  if (ids.length >= MAX_WISHLIST) throw new Error(`Danh sách yêu thích tối đa ${MAX_WISHLIST} sản phẩm`);
  return [productId, ...ids];
}

/** Id của một trang (trang tính từ 1). */
export function pageIds(ids, page, size = WISHLIST_PAGE_SIZE) {
  return ids.slice((page - 1) * size, page * size);
}

/** Thẻ sản phẩm theo đúng thứ tự trong danh sách yêu thích (API trả theo thứ tự khác). */
export function orderCards(cards, ids) {
  const byId = new Map(cards.map((card) => [card.id, card]));
  return ids.map((id) => byId.get(id)).filter(Boolean);
}
