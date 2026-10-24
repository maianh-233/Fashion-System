import { API_BASE_URL } from "./query.js";

export class PublicApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = "PublicApiError";
    this.status = status;
  }
}

/** Gọi endpoint công khai (không cần đăng nhập) và chuẩn hóa lỗi mạng / lỗi HTTP. */
export async function requestPublic(path, { signal, method = "GET", body } = {}) {
  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method, signal,
      headers: body === undefined ? { Accept: "application/json" } : { Accept: "application/json", "Content-Type": "application/json" },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (error) {
    if (error?.name === "AbortError") throw error;
    throw new PublicApiError("Không kết nối được máy chủ. Vui lòng thử lại.", 0);
  }
  const text = await response.text();
  let data;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }
  if (!response.ok) throw new PublicApiError(data?.message || "Không tải được dữ liệu sản phẩm.", response.status);
  return data;
}
