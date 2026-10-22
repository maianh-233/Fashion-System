const API_BASE_URL = (import.meta.env?.VITE_API_BASE_URL || "").replace(/\/$/, "");
const ROOT = `${API_BASE_URL}/api/storefront`;
const PATH = `${ROOT}/products`;

export class StorefrontApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = "StorefrontApiError";
    this.status = status;
  }
}

async function get(url, { signal, method = "GET", body } = {}) {
  let response;
  try {
    response = await fetch(url, {
      method, signal,
      headers: body === undefined ? { Accept: "application/json" } : { Accept: "application/json", "Content-Type": "application/json" },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (error) {
    if (error?.name === "AbortError") throw error;
    throw new StorefrontApiError("Không kết nối được máy chủ. Vui lòng thử lại.", 0);
  }
  const text = await response.text();
  let data;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }
  if (!response.ok) throw new StorefrontApiError(data?.message || "Không tải được dữ liệu sản phẩm.", response.status);
  return data;
}

/** Sản phẩm cho cửa hàng online: công khai, không cần đăng nhập. */
export const storefrontApi = {
  /** @param {URLSearchParams|string} query xem {@link buildProductQuery} */
  products: (query, options = {}) => get(`${PATH}?${query}`, options),
  filters: (options = {}) => get(`${PATH}/filters`, options),
  detail: (idOrSlug, options = {}) => get(`${PATH}/${encodeURIComponent(idOrSlug)}`, options),
  brands: (query, options = {}) => get(`${ROOT}/brands?${query}`, options),
  brand: (id, options = {}) => get(`${ROOT}/brands/${encodeURIComponent(id)}`, options),
  collections: (query, options = {}) => get(`${ROOT}/collections?${query}`, options),
  collection: (id, options = {}) => get(`${ROOT}/collections/${encodeURIComponent(id)}`, options),
  promotions: (query, options = {}) => get(`${ROOT}/promotions?${query}`, options),
  /** Giỏ trên trình duyệt (chưa đăng nhập) với giá và tồn kho hiện tại. */
  previewCart: (items, options = {}) => get(`${ROOT}/cart/preview`, { ...options, method: "POST", body: { items } }),
  paymentMethods: (options = {}) => get(`${ROOT}/payment-methods`, options),
};
