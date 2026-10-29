// Logic thuần của trang sản phẩm phía khách (dễ kiểm thử, không phụ thuộc React).

export const PAGE_SIZE = 12;

export const SEARCH_TYPES = { product: "PRODUCT", brand: "BRAND", collection: "COLLECTION", tag: "TAG" };

export const SORT_OPTIONS = [
  { value: "NEWEST", label: "Mới nhất" },
  { value: "PRICE_ASC", label: "Giá tăng dần" },
  { value: "PRICE_DESC", label: "Giá giảm dần" },
  { value: "NAME", label: "Tên A–Z" },
];

export const GENDER_LABELS = { FEMALE: "Nữ", MALE: "Nam", UNISEX: "Unisex" };

/** Tham số gọi API danh sách; bộ lọc giá chỉ gửi khi người dùng đã kéo khỏi biên. */
export function buildProductQuery({
  search = "", searchType = "product", genders = [], categoryIds = [], priceRange, priceBounds,
  inStockOnly = false, sort = "NEWEST", page = 1, size = PAGE_SIZE,
} = {}) {
  const query = new URLSearchParams();
  const keyword = search.trim();
  if (keyword) {
    query.set("keyword", keyword);
    query.set("searchType", SEARCH_TYPES[searchType] || "PRODUCT");
  }
  genders.forEach((gender) => query.append("gender", gender));
  categoryIds.forEach((id) => query.append("categoryId", id));
  if (priceRange && priceBounds) {
    if (priceRange[0] > priceBounds[0]) query.set("minPrice", String(priceRange[0]));
    if (priceRange[1] < priceBounds[1]) query.set("maxPrice", String(priceRange[1]));
  }
  if (inStockOnly) query.set("inStock", "true");
  if (sort && sort !== "NEWEST") query.set("sort", sort);
  query.set("page", String(Math.max(page, 1) - 1));
  query.set("size", String(size));
  return query;
}

/** Biên của thanh trượt giá: làm tròn ra ngoài theo bước để giá thật luôn nằm trong khoảng. */
export function priceBoundsOf(filters, step = 50000) {
  if (filters?.minPrice == null || filters?.maxPrice == null) return [0, 5000000];
  const low = Math.floor(Number(filters.minPrice) / step) * step;
  const high = Math.ceil(Number(filters.maxPrice) / step) * step;
  return [low, Math.max(high, low + step)];
}

export const formatPrice = (value) => `${Number(value || 0).toLocaleString("vi-VN")} ₫`;

/** Bỏ tiền tố dữ liệu mẫu "DEMO " khỏi tên tag khi hiển thị. */
export const tagLabel = (tag) => String(tag || "").replace(/^DEMO\s+/i, "");

const COLOR_SWATCHES = {
  "đen": "#1f1f22", "trắng": "#f4f1ea", "be": "#d8c3a5", "navy": "#1f2a44", "nâu": "#7a5236",
  "xanh rêu": "#5b6b3f", "xám": "#8a8a8f", "đỏ": "#b3261e", "hồng": "#e3a6b4", "vàng": "#e0b84f",
  "xanh dương": "#3d6db5", "xanh lá": "#4f8a4b", "kem": "#efe6d2", "tím": "#7c5c9c", "cam": "#e07b39",
};

/** Màu hiển thị cho ô chọn màu; màu chưa biết dùng xám trung tính. */
export function colorSwatch(color) {
  return COLOR_SWATCHES[String(color || "").trim().toLowerCase()] || "#9a9a9f";
}

/** Ảnh sản phẩm; chưa có ảnh thì dùng ảnh giữ chỗ dựng sẵn (SVG) mang tên sản phẩm. */
export function productImage(url, name = "") {
  if (url) return url;
  const initials = String(name).split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0]).join("").toUpperCase() || "L";
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 750"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">`
    + `<stop offset="0" stop-color="#2a2622"/><stop offset="1" stop-color="#151413"/></linearGradient></defs>`
    + `<rect width="600" height="750" fill="url(#g)"/><text x="300" y="400" text-anchor="middle" font-family="Georgia,serif" `
    + `font-size="150" fill="#d6a95f" fill-opacity="0.85">${escapeXml(initials)}</text><text x="300" y="470" `
    + `text-anchor="middle" font-family="Arial,sans-serif" font-size="22" letter-spacing="8" fill="#a1a1aa">LUNARIA</text></svg>`;
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

function escapeXml(value) {
  return value.replace(/[<>&"']/g, (c) => ({ "<": "&lt;", ">": "&gt;", "&": "&amp;", '"': "&quot;", "'": "&apos;" }[c]));
}

/** Biến thể theo màu + size. */
export function findVariant(variants = [], color, size) {
  return variants.find((v) => v.color === color && v.size === size) || null;
}

/** Trạng thái từng size với màu đang chọn: không có biến thể, hết hàng, hoặc còn hàng. */
export function sizeStates(variants = [], sizes = [], color) {
  return sizes.map((size) => {
    const variant = findVariant(variants, color, size);
    return { size, exists: Boolean(variant), available: variant ? variant.available : 0 };
  });
}

/** Lựa chọn ban đầu: biến thể còn hàng đầu tiên, nếu không có thì biến thể đầu tiên. */
export function initialSelection(variants = []) {
  const first = variants.find((v) => v.available > 0) || variants[0];
  return first ? { color: first.color, size: first.size } : { color: null, size: null };
}

/** Đổi màu: giữ size nếu màu mới có size đó còn hàng, không thì chọn size còn hàng đầu tiên của màu mới. */
export function selectColor(variants = [], sizes = [], color, currentSize) {
  const current = findVariant(variants, color, currentSize);
  if (current && current.available > 0) return { color, size: currentSize };
  const inStock = sizeStates(variants, sizes, color).find((s) => s.available > 0);
  const any = sizeStates(variants, sizes, color).find((s) => s.exists);
  return { color, size: (inStock || any)?.size ?? null };
}

/** Thông điệp tồn kho cho khách (không lộ số lớn). */
export function stockLabel(variant) {
  if (!variant) return "Phiên bản này không có";
  if (variant.available <= 0) return "Tạm hết hàng";
  if (variant.available <= 5) return `Chỉ còn ${variant.available} sản phẩm`;
  return "Còn hàng";
}

/** Ảnh hiển thị: ảnh của màu đang chọn trước, sau đó ảnh chung; không có thì ảnh giữ chỗ. */
export function galleryImages(images = [], color, name) {
  const ordered = [...images.filter((img) => img.color && img.color === color), ...images.filter((img) => !img.color),
    ...images.filter((img) => img.color && img.color !== color)];
  const urls = ordered.map((img) => img.url);
  return urls.length ? urls : [productImage(null, name)];
}

export const SEASON_LABELS = { SPRING: "Xuân", SUMMER: "Hè", AUTUMN: "Thu", FALL: "Thu", WINTER: "Đông" };

/** "Thu · 2026" từ mùa và năm của bộ sưu tập. */
export function seasonLabel(season, year) {
  const label = season ? SEASON_LABELS[String(season).toUpperCase()] || season : null;
  return [label, year].filter(Boolean).join(" · ");
}

export const PROMOTION_TYPE_LABELS = {
  ORDER: "Giảm trên đơn hàng",
  PRODUCT: "Giảm theo sản phẩm",
  POINT_REDEMPTION: "Đổi điểm lấy voucher",
  BIRTHDAY: "Quà sinh nhật",
};

const money = (value) => `${Number(value || 0).toLocaleString("vi-VN")}đ`;

/** Mức giảm hiển thị trên thẻ khuyến mãi. */
export function promotionValueLabel(promotion) {
  if (!promotion) return "";
  const value = promotion.discountType === "PERCENT"
    ? `Giảm ${Number(promotion.discountValue)}%`
    : `Giảm ${money(promotion.discountValue)}`;
  const cap = promotion.discountType === "PERCENT" && promotion.maxDiscount != null
    ? ` (tối đa ${money(promotion.maxDiscount)})` : "";
  return value + cap;
}

/** Điều kiện áp dụng ngắn gọn cho khách. */
export function promotionConditions(promotion) {
  const lines = [];
  if (promotion.promotionType === "POINT_REDEMPTION" && promotion.pointsRequired != null)
    lines.push(`Đổi ${Number(promotion.pointsRequired).toLocaleString("vi-VN")} điểm tích lũy để nhận voucher`);
  if (promotion.promotionType === "BIRTHDAY")
    lines.push("Tự động tặng vào ngày 1 tháng sinh nhật, dùng đến hết tháng (cần lưu ngày sinh trong hồ sơ)");
  if (promotion.minOrderValue != null && Number(promotion.minOrderValue) > 0)
    lines.push(`Áp dụng cho đơn từ ${money(promotion.minOrderValue)}`);
  if (promotion.promotionType === "PRODUCT")
    lines.push(`Áp dụng cho ${promotion.productCount} sản phẩm được chọn`);
  if (promotion.tiers?.length) lines.push(`Dành cho hạng ${promotion.tiers.join(", ")}`);
  if (promotion.usagePerUser != null) lines.push(`Mỗi khách dùng tối đa ${promotion.usagePerUser} lần`);
  return lines;
}

/** Trạng thái hiển thị: hết lượt, sắp diễn ra, hoặc đang áp dụng. */
export function promotionStatus(promotion) {
  if (promotion.soldOut) return { key: "sold-out", label: "Đã hết lượt" };
  if (promotion.status === "SCHEDULED") return { key: "scheduled", label: "Sắp diễn ra" };
  return { key: "active", label: "Đang áp dụng" };
}

export const formatDate = (value) => (value ? new Date(value).toLocaleDateString("vi-VN") : "");

/** "Số nhà đường, phường, quận, tỉnh" từ địa chỉ của khách. */
export const formatAddress = (address) => [address?.addressLine, address?.ward, address?.district, address?.province]
  .filter(Boolean).join(", ");
