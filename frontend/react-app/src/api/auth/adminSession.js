const API_BASE_URL = (import.meta.env?.VITE_API_BASE_URL || "").replace(/\/$/, "");
const LEGACY_STORAGE_KEY = "lunaria_admin_session";
let adminSession = null;
let refreshPromise = null;

// Xóa token do phiên bản cũ từng persist. Access token từ đây chỉ tồn tại trong memory.
try {
  window.localStorage.removeItem(LEGACY_STORAGE_KEY);
} catch {
  // Storage có thể bị vô hiệu hóa; điều này không ảnh hưởng phiên cookie.
}

/** Lỗi từ API yêu cầu phiên nhân viên, giữ status để phân biệt 401 và 403. */
export class AdminSessionError extends Error {
  constructor(message, status, data) {
    super(message);
    this.name = "AdminSessionError";
    this.status = status;
    this.data = data;
  }
}

export function getAdminSession() {
  return adminSession;
}

export function saveAdminSession(session) {
  adminSession = typeof session?.token === "string" && session.token ? session : null;
  window.dispatchEvent(new Event("lunaria:admin-session"));
}

export function clearAdminSession() {
  adminSession = null;
  try {
    window.localStorage.removeItem(LEGACY_STORAGE_KEY);
  } catch {
    // Không cần storage để xóa phiên memory.
  }
  window.dispatchEvent(new Event("lunaria:admin-session"));
}

async function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
        method: "POST",
        credentials: "include",
        headers: { Accept: "application/json", "X-Requested-With": "XMLHttpRequest" },
      });
      const data = response.status === 204 ? null : await response.json().catch(() => null);
      if (!response.ok || typeof data?.token !== "string") {
        clearAdminSession();
        throw new AdminSessionError(
          data?.message || data?.detail || "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
          response.status || 401,
          data,
        );
      }
      saveAdminSession(data);
      return data.token;
    })().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

async function sendAdminRequest(path, { method, body, signal }, token) {
  const isMultipart = typeof FormData !== "undefined" && body instanceof FormData;
  return fetch(`${API_BASE_URL}${path}`, {
    method,
    credentials: "include",
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${token}`,
      ...(body === undefined || isMultipart ? {} : { "Content-Type": "application/json" }),
    },
    body: body === undefined ? undefined : isMultipart ? body : JSON.stringify(body),
    signal,
  });
}

/** Gửi API bằng access token memory; 401 sẽ refresh single-flight rồi retry đúng một lần. */
export async function requestAdmin(path, { method = "GET", body, signal } = {}) {
  let token = getAdminSession()?.token;
  if (!token) token = await refreshAccessToken();

  let response = await sendAdminRequest(path, { method, body, signal }, token);
  if (response.status === 401) {
    // Một request đồng thời khác có thể đã refresh xong trong lúc request này đang bay.
    // Khi đó dùng access token mới ngay, không rotate refresh cookie thêm lần nữa.
    const latestToken = getAdminSession()?.token;
    token = latestToken && latestToken !== token ? latestToken : await refreshAccessToken();
    response = await sendAdminRequest(path, { method, body, signal }, token);
  }

  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) {
    // Chỉ retry một lần. 401 lần hai chứng tỏ access/identity không còn hợp lệ.
    if (response.status === 401) clearAdminSession();
    throw new AdminSessionError(
      data?.message || data?.detail || data?.error || `Yêu cầu thất bại (${response.status})`,
      response.status,
      data,
    );
  }
  // Spring Data's stable PagedModel keeps pagination metadata under `page`.
  // Keep the existing UI contract for all paginated admin endpoints.
  return data?.page && Array.isArray(data.content)
    ? { ...data, ...data.page }
    : data;
}

/** Gửi request admin và giữ nguyên binary response cho các file export. */
export async function requestAdminBlob(path, { signal } = {}) {
  let token = getAdminSession()?.token;
  if (!token) token = await refreshAccessToken();
  let response = await sendAdminRequest(path, { method: "GET", signal }, token);
  if (response.status === 401) {
    token = await refreshAccessToken();
    response = await sendAdminRequest(path, { method: "GET", signal }, token);
  }
  if (!response.ok) {
    const data = await response.json().catch(() => null);
    if (response.status === 401) clearAdminSession();
    throw new AdminSessionError(data?.message || data?.detail || ("Yêu cầu thất bại (" + response.status + ")"), response.status, data);
  }
  return { blob: await response.blob(), contentDisposition: response.headers.get("Content-Disposition") };
}

export const getAdminProfile = (options = {}) => requestAdmin("/api/users/me", options);
export const updateAdminProfile = (body) => requestAdmin("/api/users/me", { method: "PATCH", body });
export const changeAdminPassword = (body) => requestAdmin("/api/users/me/password", { method: "POST", body });
