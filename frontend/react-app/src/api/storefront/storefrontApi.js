import { requestPublic } from "../http/publicRequest.js";

const ROOT = "/api/storefront";
const PATH = `${ROOT}/products`;

/** Sản phẩm cho cửa hàng online: công khai, không cần đăng nhập. */
export const storefrontApi = {
  /** @param {URLSearchParams|string} query xem {@link buildProductQuery} */
  products: (query, options = {}) => requestPublic(`${PATH}?${query}`, options),
  filters: (options = {}) => requestPublic(`${PATH}/filters`, options),
  detail: (idOrSlug, options = {}) => requestPublic(`${PATH}/${encodeURIComponent(idOrSlug)}`, options),
  brands: (query, options = {}) => requestPublic(`${ROOT}/brands?${query}`, options),
  brand: (id, options = {}) => requestPublic(`${ROOT}/brands/${encodeURIComponent(id)}`, options),
  collections: (query, options = {}) => requestPublic(`${ROOT}/collections?${query}`, options),
  collection: (id, options = {}) => requestPublic(`${ROOT}/collections/${encodeURIComponent(id)}`, options),
  promotions: (query, options = {}) => requestPublic(`${ROOT}/promotions?${query}`, options),
  /** Giỏ trên trình duyệt (chưa đăng nhập) với giá và tồn kho hiện tại. */
  previewCart: (items, options = {}) => requestPublic(`${ROOT}/cart/preview`, { ...options, method: "POST", body: { items } }),
  paymentMethods: (options = {}) => requestPublic(`${ROOT}/payment-methods`, options),
  /** Cửa hàng đang hoạt động (trang hệ thống cửa hàng). */
  stores: (options = {}) => requestPublic(`${ROOT}/stores`, options),
  /** Cửa hàng còn hàng tại quầy của một biến thể: [{store, stock: IN_STOCK|LOW_STOCK}]. */
  variantStores: (variantId, options = {}) => requestPublic(`${ROOT}/variants/${encodeURIComponent(variantId)}/stores`, options),
  /** Điểm trung bình và phân bố sao của đánh giá đang hiển thị: {average, count, stars[5→1], withImages}. */
  reviewSummary: (productId, options = {}) => requestPublic(`${PATH}/${encodeURIComponent(productId)}/reviews/summary`, options),
  /** Đánh giá đang hiển thị, mới nhất trước; lọc theo số sao và/hoặc chỉ đánh giá có ảnh. */
  reviews: (productId, { rating, withImages = false, page = 0, size = 5 } = {}, options = {}) => {
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    if (rating) query.set("rating", String(rating));
    if (withImages) query.set("withImages", "true");
    return requestPublic(`${PATH}/${encodeURIComponent(productId)}/reviews?${query}`, options);
  },
};
