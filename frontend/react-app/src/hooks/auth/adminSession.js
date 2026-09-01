const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");
const PROFILE_API_URL = `${API_BASE_URL}/api/users/me`;
const STORAGE_KEY = "lunaria_admin_session";

export function getAdminSession() {
  try {
    return JSON.parse(window.localStorage.getItem(STORAGE_KEY) || "null");
  } catch {
    return null;
  }
}

export function saveAdminSession(session) {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  window.dispatchEvent(new Event("lunaria:admin-session"));
}

export function clearAdminSession() {
  window.localStorage.removeItem(STORAGE_KEY);
  window.dispatchEvent(new Event("lunaria:admin-session"));
}

async function profileRequest(path = "", { method = "GET", body } = {}) {
  const token = getAdminSession()?.token;
  if (!token) throw new Error("Phiên đăng nhập không tồn tại. Vui lòng đăng nhập lại.");

  const response = await fetch(`${PROFILE_API_URL}${path}`, {
    method,
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${token}`,
      ...(body === undefined ? {} : { "Content-Type": "application/json" }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 401) clearAdminSession();
    throw new Error(data?.message || data?.detail || data?.error || `Yêu cầu thất bại (${response.status})`);
  }
  return data;
}

export const getAdminProfile = () => profileRequest();
export const updateAdminProfile = (body) => profileRequest("", { method: "PATCH", body });
export const changeAdminPassword = (body) => profileRequest("/password", { method: "POST", body });
