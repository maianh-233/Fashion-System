// Logic thuần của ví voucher: chuyển dữ liệu khuyến mãi sang dạng thẻ storefront và trạng thái voucher đã đổi.

/** API khách trả hạng dạng {id, code, name}; thẻ storefront cần danh sách tên. */
export function toCardPromotion(promotion) {
  if (!promotion) return null;
  return { ...promotion, tiers: (promotion.tiers || []).map((tier) => (typeof tier === "string" ? tier : tier?.name)).filter(Boolean) };
}

/** Khách có đủ điểm để đổi không. */
export function canRedeem(promotion, points) {
  return promotion?.pointsRequired != null && Number(points || 0) >= Number(promotion.pointsRequired);
}

/** Số điểm còn thiếu để đổi (0 khi đã đủ). */
export function missingPoints(promotion, points) {
  return Math.max(0, Number(promotion?.pointsRequired || 0) - Number(points || 0));
}

/**
 * Trạng thái voucher trong ví: đã dùng, hết hạn, hoặc còn dùng được. Voucher sinh nhật có hạn riêng
 * (validFrom/expiresAt) thay cho thời gian của khuyến mãi.
 */
export function voucherState(item, now = new Date()) {
  if (item?.usedOrderId) return { key: "used", label: "Đã sử dụng" };
  const promotion = item?.promotion;
  if (item?.expiresAt) {
    if (!promotion || promotion.status === "INACTIVE" || new Date(item.expiresAt) <= now)
      return { key: "expired", label: "Hết hiệu lực" };
    return { key: "active", label: "Chưa sử dụng" };
  }
  if (!promotion || promotion.status !== "ACTIVE" || (promotion.endDate && new Date(promotion.endDate) <= now))
    return { key: "expired", label: "Hết hiệu lực" };
  return { key: "active", label: "Chưa sử dụng" };
}

/** Ngày cuối dùng được của voucher có hạn riêng ({@code expiresAt} là 0h ngày kế tiếp). */
export function lastUsableDay(expiresAt) {
  return expiresAt ? new Date(new Date(expiresAt).getTime() - 1) : null;
}

/** Voucher trong ví hiển thị như khuyến mãi, nhưng thời gian là hạn riêng của voucher sinh nhật. */
export function ownedCardPromotion(item) {
  const promotion = toCardPromotion(item?.promotion);
  if (!promotion || !item?.expiresAt) return promotion;
  return { ...promotion, startDate: item.validFrom, endDate: lastUsableDay(item.expiresAt)?.toISOString() };
}

export const POINT_ENTRY_LABELS = {
  EARN: "Tích điểm",
  REFUND_REVERSAL: "Trừ do hoàn tiền",
  REDEEM: "Đổi voucher",
  EXPIRE: "Hết hạn",
  ADJUST: "Điều chỉnh",
};

/** Điểm hiển thị có dấu: "+120" hoặc "-50". */
export const signedPoints = (points) => `${Number(points) > 0 ? "+" : ""}${Number(points || 0).toLocaleString("vi-VN")}`;

export const formatPoints = (value) => `${Number(value || 0).toLocaleString("vi-VN")} điểm`;
