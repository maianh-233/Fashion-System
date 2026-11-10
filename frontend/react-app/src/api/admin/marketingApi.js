import { requestAdmin, requestAdminBlob } from "../http/adminSession.js";

const BASE = "/api/admin/marketing-campaigns";

function qs(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    query.set(key, String(value));
  });
  const text = query.toString();
  return text ? `?${text}` : "";
}

/** Chiến dịch marketing (phạm vi Global, trạng thái và luật tự duyệt kiểm tra ở backend). */
export const marketingApi = {
  lookups: (options = {}) => requestAdmin(`${BASE}/lookups`, options),
  list: (params = {}, options = {}) => requestAdmin(`${BASE}${qs(params)}`, options),
  detail: (id, options = {}) => requestAdmin(`${BASE}/${id}`, options),
  create: (body) => requestAdmin(BASE, { method: "POST", body }),
  update: (id, body) => requestAdmin(`${BASE}/${id}`, { method: "PUT", body }),
  submit: (id) => requestAdmin(`${BASE}/${id}/submit`, { method: "POST" }),
  withdraw: (id) => requestAdmin(`${BASE}/${id}/withdraw`, { method: "POST" }),
  approve: (id, note) => requestAdmin(`${BASE}/${id}/approve`, { method: "POST", body: { note: note || null } }),
  reject: (id, reason) => requestAdmin(`${BASE}/${id}/reject`, { method: "POST", body: { reason } }),
  reopen: (id, reason) => requestAdmin(`${BASE}/${id}/reopen`, { method: "POST", body: { reason } }),
  cancel: (id, reason) => requestAdmin(`${BASE}/${id}/cancel`, { method: "POST", body: { reason } }),
  candidates: (params = {}, options = {}) => requestAdmin(`${BASE}/offer-candidates${qs(params)}`, options),
  link: (id, type, itemId) => requestAdmin(`${BASE}/${id}/offers`, { method: "POST", body: { type, itemId } }),
  unlink: (id, offerId) => requestAdmin(`${BASE}/${id}/offers/${offerId}`, { method: "DELETE" }),
  addExpense: (id, body) => requestAdmin(`${BASE}/${id}/expenses`, { method: "POST", body }),
  updateExpense: (id, expenseId, body) => requestAdmin(`${BASE}/${id}/expenses/${expenseId}`, { method: "PUT", body }),
  deleteExpense: (id, expenseId) => requestAdmin(`${BASE}/${id}/expenses/${expenseId}`, { method: "DELETE" }),
  previewAudience: (criteria) => requestAdmin(`${BASE}/audience-preview`, { method: "POST", body: criteria }),
  createDispatch: (id, body) => requestAdmin(`${BASE}/${id}/dispatches`, { method: "POST", body }),
  updateDispatch: (id, dispatchId, body) => requestAdmin(`${BASE}/${id}/dispatches/${dispatchId}`, { method: "PUT", body }),
  approveDispatch: (id, dispatchId) => requestAdmin(`${BASE}/${id}/dispatches/${dispatchId}/approve`, { method: "POST" }),
  rejectDispatch: (id, dispatchId, reason) => requestAdmin(`${BASE}/${id}/dispatches/${dispatchId}/reject`, { method: "POST", body: { reason } }),
  cancelDispatch: (id, dispatchId, reason) => requestAdmin(`${BASE}/${id}/dispatches/${dispatchId}/cancel`, { method: "POST", body: { reason } }),
  retryDispatch: (id, dispatchId) => requestAdmin(`${BASE}/${id}/dispatches/${dispatchId}/retry-failed`, { method: "POST" }),
  recipients: (id, dispatchId, params = {}, options = {}) =>
    requestAdmin(`${BASE}/${id}/dispatches/${dispatchId}/recipients${qs(params)}`, options),
  performance: (id, options = {}) => requestAdmin(`${BASE}/${id}/performance`, options),
  exportPerformance: (id) => requestAdminBlob(`${BASE}/${id}/performance/export`),
  compare: (ids, options = {}) => requestAdmin(`${BASE}/compare${qs({ ids: ids.join(",") })}`, options),
  calendar: (from, to, options = {}) => requestAdmin(`${BASE}/calendar${qs({ from, to })}`, options),
};
