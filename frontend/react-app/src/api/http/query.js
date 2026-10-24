/** Gốc của backend; rỗng khi đi qua proxy của Vite cùng origin. */
export const API_BASE_URL = (import.meta.env?.VITE_API_BASE_URL || "").replace(/\/$/, "");

/** Dựng query string từ object, bỏ qua giá trị rỗng; trả về "" hoặc "?a=1&b=2". */
export function queryString(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") query.set(key, String(value));
  });
  const text = query.toString();
  return text ? `?${text}` : "";
}
