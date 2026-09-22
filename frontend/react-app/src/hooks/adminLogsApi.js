import { requestAdmin } from "./auth/adminSession";

export function queryString(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") query.set(key, value);
  });
  const result = query.toString();
  return result ? `?${result}` : "";
}

export function fetchAdminLogs(params = {}, options = {}) {
  return requestAdmin(`/api/admin/audit-logs${queryString(params)}`, options);
}

export function normalizeAdminLogsPage(response) {
  return {
    rows: Array.isArray(response?.content) ? response.content : [],
    page: Number(response?.number ?? 0) + 1,
    totalPages: Math.max(1, Number(response?.totalPages ?? 1)),
    totalElements: Number(response?.totalElements ?? 0),
  };
}
