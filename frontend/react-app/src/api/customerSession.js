import { refreshCustomerSession } from "./auth/authApi.js";

const API_BASE_URL = (import.meta.env?.VITE_API_BASE_URL || "").replace(/\/$/, "");
export const CUSTOMER_TOKEN_KEY = "lunaria_customer_access_token";
export const CUSTOMER_PROFILE_KEY = "lunaria_customer_profile";
export const CUSTOMER_SESSION_EVENT = "lunaria:customer-session";

const storage = () => {
  try {
    return window.sessionStorage;
  } catch {
    return null;
  }
};

export function getCustomerToken() {
  return storage()?.getItem(CUSTOMER_TOKEN_KEY) || null;
}

export function getCustomerProfile() {
  try {
    return JSON.parse(storage()?.getItem(CUSTOMER_PROFILE_KEY) || "null");
  } catch {
    return null;
  }
}

/** Lưu phiên sau khi đăng nhập/đăng ký/làm mới và báo cho header, giỏ hàng. */
export function saveCustomerSession(response) {
  if (!response?.token || !response?.customer) throw new Error("Backend không trả về phiên customer hợp lệ.");
  storage()?.setItem(CUSTOMER_TOKEN_KEY, response.token);
  storage()?.setItem(CUSTOMER_PROFILE_KEY, JSON.stringify(response.customer));
  window.dispatchEvent(new Event(CUSTOMER_SESSION_EVENT));
}

export function clearCustomerSession() {
  storage()?.removeItem(CUSTOMER_TOKEN_KEY);
  storage()?.removeItem(CUSTOMER_PROFILE_KEY);
  window.dispatchEvent(new Event(CUSTOMER_SESSION_EVENT));
}

export class CustomerApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.name = "CustomerApiError";
    this.status = status;
    this.data = data;
  }
}

let refreshing = null;
/** Làm mới access token bằng refresh cookie; nhiều request cùng lúc chỉ làm mới một lần. */
async function refreshToken() {
  if (!refreshing) {
    refreshing = refreshCustomerSession()
      .then((response) => {
        saveCustomerSession(response);
        return response.token;
      })
      .catch(() => {
        clearCustomerSession();
        return null;
      })
      .finally(() => {
        refreshing = null;
      });
  }
  return refreshing;
}

async function send(path, { method = "GET", body, signal }, token) {
  const headers = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (token) headers.Authorization = `Bearer ${token}`;
  return fetch(`${API_BASE_URL}${path}`, {
    method, headers, signal, credentials: "include",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

/**
 * Gọi API của khách đã đăng nhập. Token hết hạn thì làm mới một lần rồi gọi lại; vẫn 401 thì xóa phiên và ném lỗi
 * status 401 để trang chuyển tới đăng nhập.
 */
export async function requestCustomer(path, options = {}) {
  let token = getCustomerToken();
  if (!token) token = await refreshToken();
  if (!token) throw new CustomerApiError("Vui lòng đăng nhập để tiếp tục.", 401);

  let response;
  try {
    response = await send(path, options, token);
    if (response.status === 401) {
      const renewed = await refreshToken();
      if (!renewed) throw new CustomerApiError("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.", 401);
      response = await send(path, options, renewed);
    }
  } catch (error) {
    if (error?.name === "AbortError" || error instanceof CustomerApiError) throw error;
    throw new CustomerApiError("Không kết nối được máy chủ. Vui lòng thử lại.", 0);
  }

  const text = await response.text();
  let data;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }
  if (!response.ok) {
    if (response.status === 401) clearCustomerSession();
    throw new CustomerApiError(data?.message || "Không thực hiện được thao tác.", response.status, data);
  }
  return data;
}
