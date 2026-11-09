import { requestAdmin } from "../http/adminSession.js";

const BASE = "/api/admin/tasks";

function qs(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    query.set(key, String(value));
  });
  const text = query.toString();
  return text ? `?${text}` : "";
}

/** Quản lý công việc (quyền trên từng việc và phạm vi cửa hàng kiểm tra ở backend). */
export const taskApi = {
  lookups: (options = {}) => requestAdmin(`${BASE}/lookups`, options),
  list: (params = {}, options = {}) => requestAdmin(`${BASE}${qs(params)}`, options),
  mine: (params = {}, options = {}) => requestAdmin(`${BASE}/mine${qs(params)}`, options),
  stats: (params = {}, options = {}) => requestAdmin(`${BASE}/stats${qs(params)}`, options),
  detail: (id, options = {}) => requestAdmin(`${BASE}/${id}`, options),
  create: (body) => requestAdmin(BASE, { method: "POST", body }),
  update: (id, body) => requestAdmin(`${BASE}/${id}`, { method: "PUT", body }),
  start: (id) => requestAdmin(`${BASE}/${id}/start`, { method: "POST" }),
  complete: (id, note) => requestAdmin(`${BASE}/${id}/complete`, { method: "POST", body: { note } }),
  giveBack: (id, reason) => requestAdmin(`${BASE}/${id}/return`, { method: "POST", body: { reason } }),
  cancel: (id, reason) => requestAdmin(`${BASE}/${id}/cancel`, { method: "POST", body: { reason } }),
  toggleItem: (id, itemId, done) => requestAdmin(`${BASE}/${id}/checklist/${itemId}`, { method: "PUT", body: { done } }),
  comment: (id, content) => requestAdmin(`${BASE}/${id}/comments`, { method: "POST", body: { content } }),
};
