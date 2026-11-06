import { requestAdmin } from "../http/adminSession.js";

const BASE = "/api/admin/stocktakes";

function qs(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    query.set(key, String(value));
  });
  const text = query.toString();
  return text ? `?${text}` : "";
}

/** Kiểm kê kho (phạm vi cửa hàng kiểm tra ở backend). */
export const stocktakeApi = {
  lookups: (options = {}) => requestAdmin(`${BASE}/lookups`, options),
  list: (params = {}, options = {}) => requestAdmin(`${BASE}${qs(params)}`, options),
  detail: (id, options = {}) => requestAdmin(`${BASE}/${id}`, options),
  items: (id, params = {}, options = {}) => requestAdmin(`${BASE}/${id}/items${qs(params)}`, options),
  create: (body) => requestAdmin(BASE, { method: "POST", body }),
  scan: (id, code, quantity) => requestAdmin(`${BASE}/${id}/scan`, { method: "POST", body: { code, quantity } }),
  setCount: (id, itemId, countedQuantity, note) =>
    requestAdmin(`${BASE}/${id}/items/${itemId}`, { method: "PUT", body: { countedQuantity, note } }),
  submit: (id, zeroUncounted) => requestAdmin(`${BASE}/${id}/submit`, { method: "POST", body: { zeroUncounted } }),
  approve: (id, note) => requestAdmin(`${BASE}/${id}/approve`, { method: "POST", body: { note } }),
  reject: (id, reason) => requestAdmin(`${BASE}/${id}/reject`, { method: "POST", body: { reason } }),
  cancel: (id) => requestAdmin(`${BASE}/${id}/cancel`, { method: "POST" }),
  report: (params = {}, options = {}) => requestAdmin(`${BASE}/report${qs(params)}`, options),
};
