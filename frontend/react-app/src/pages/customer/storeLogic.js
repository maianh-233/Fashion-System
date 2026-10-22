// Logic thuần của trang hệ thống cửa hàng và tra cửa hàng còn hàng.

export const STORE_FALLBACK_IMAGE = "https://images.unsplash.com/photo-1441986300917-64674bd600d8?q=85&w=900&auto=format&fit=crop";

export const STOCK_LABELS = { IN_STOCK: "Còn hàng", LOW_STOCK: "Sắp hết" };

/** Bỏ dấu và viết thường để tìm kiếm. */
export const normalizeText = (value) => String(value || "").normalize("NFD").replace(/[̀-ͯ]/g, "")
  .replace(/đ/g, "d").replace(/Đ/g, "D").toLowerCase();

/** Khu vực hiển thị: phần cuối địa chỉ (tỉnh/thành), ví dụ "TP. Hồ Chí Minh". */
export function storeArea(address) {
  const parts = String(address || "").split(",").map((part) => part.trim()).filter(Boolean);
  return parts.length ? parts[parts.length - 1] : "";
}

/** Cửa hàng từ API thêm tọa độ [lat, lng] và khu vực cho bản đồ; bỏ cửa hàng thiếu tọa độ. */
export function toMapStores(stores = []) {
  return stores
    .filter((store) => store.latitude != null && store.longitude != null)
    .map((store) => ({ ...store, coordinates: [Number(store.latitude), Number(store.longitude)], area: storeArea(store.address) }));
}

export function filterStores(stores, search) {
  const term = normalizeText(search.trim());
  if (!term) return stores;
  return stores.filter((store) => normalizeText(`${store.name} ${store.address} ${store.area}`).includes(term));
}

export function distanceInKm([lat1, lon1], [lat2, lon2]) {
  const toRadians = (value) => (value * Math.PI) / 180;
  const latDelta = toRadians(lat2 - lat1);
  const lonDelta = toRadians(lon2 - lon1);
  const a = Math.sin(latDelta / 2) ** 2
    + Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.sin(lonDelta / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

/** Gắn khoảng cách và sắp gần trước khi có vị trí khách; không có vị trí thì giữ nguyên. */
export function sortByDistance(stores, location, coordinatesOf = (store) => store.coordinates) {
  if (!location) return stores;
  return stores
    .map((store) => ({ ...store, distance: distanceInKm(location, coordinatesOf(store)) }))
    .sort((a, b) => a.distance - b.distance);
}

export const directionsUrl = (latitude, longitude) =>
  `https://www.google.com/maps/dir/?api=1&destination=${Number(latitude)},${Number(longitude)}`;

export const telHref = (phone) => `tel:${String(phone || "").replace(/[^\d+]/g, "")}`;

/** Thoát HTML cho popup Leaflet (dữ liệu cửa hàng do admin nhập). */
export const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (char) => ({
  "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
}[char]));

/** Chỉ nhận ảnh http(s) để chèn vào popup. */
export const safeImageUrl = (url) => (/^https?:\/\//i.test(String(url || "")) ? url : STORE_FALLBACK_IMAGE);
