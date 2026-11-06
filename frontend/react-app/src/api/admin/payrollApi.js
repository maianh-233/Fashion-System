import { requestAdmin, requestAdminBlob } from "../http/adminSession.js";

const PATH = "/api/admin/payroll";

function qs(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    query.set(key, String(value));
  });
  const text = query.toString();
  return text ? `?${text}` : "";
}

/** Tính lương cho HR (phạm vi cửa hàng / bảo mật kiểm tra ở backend). */
export const payrollApi = {
  lookups: (options = {}) => requestAdmin(`${PATH}/lookups`, options),
  updateSettings: (body) => requestAdmin(`${PATH}/settings`, { method: "PUT", body }),
  createComponent: (body) => requestAdmin(`${PATH}/components`, { method: "POST", body }),
  updateComponent: (id, body) => requestAdmin(`${PATH}/components/${id}`, { method: "PUT", body }),

  salaries: (params = {}, options = {}) => requestAdmin(`${PATH}/salaries${qs(params)}`, options),
  salary: (userId, options = {}) => requestAdmin(`${PATH}/salaries/${userId}`, options),
  createStructure: (userId, body) => requestAdmin(`${PATH}/salaries/${userId}/structures`, { method: "POST", body }),
  updateStructure: (id, body) => requestAdmin(`${PATH}/structures/${id}`, { method: "PUT", body }),

  dashboard: (params = {}, options = {}) => requestAdmin(`${PATH}/dashboard${qs(params)}`, options),
  periods: (params = {}, options = {}) => requestAdmin(`${PATH}/periods${qs(params)}`, options),
  createPeriod: (body) => requestAdmin(`${PATH}/periods`, { method: "POST", body }),
  period: (id, options = {}) => requestAdmin(`${PATH}/periods/${id}`, options),
  periodRecords: (id, params = {}, options = {}) => requestAdmin(`${PATH}/periods/${id}/records${qs(params)}`, options),
  sync: (id) => requestAdmin(`${PATH}/periods/${id}/sync`, { method: "POST" }),
  calculate: (id) => requestAdmin(`${PATH}/periods/${id}/calculate`, { method: "POST" }),
  submit: (id, note) => requestAdmin(`${PATH}/periods/${id}/submit`, { method: "POST", body: { note } }),
  approve: (id, note) => requestAdmin(`${PATH}/periods/${id}/approve`, { method: "POST", body: { note } }),
  reject: (id, reason) => requestAdmin(`${PATH}/periods/${id}/reject`, { method: "POST", body: { reason } }),
  cancel: (id, reason) => requestAdmin(`${PATH}/periods/${id}/cancel`, { method: "POST", body: { reason } }),
  pay: (id, body) => requestAdmin(`${PATH}/periods/${id}/pay`, { method: "POST", body }),
  exportPeriod: (id) => requestAdminBlob(`${PATH}/periods/${id}/export`),

  record: (id, options = {}) => requestAdmin(`${PATH}/records/${id}`, options),
  addItem: (id, body) => requestAdmin(`${PATH}/records/${id}/items`, { method: "POST", body }),
  removeItem: (id, itemId, reason) => requestAdmin(`${PATH}/records/${id}/items/${itemId}/remove`, { method: "POST", body: { reason } }),
  confirmDays: (id, body) => requestAdmin(`${PATH}/records/${id}/confirm-days`, { method: "POST", body }),
  addAdjustment: (id, body) => requestAdmin(`${PATH}/records/${id}/adjustments`, { method: "POST", body }),
  cancelAdjustment: (id, adjustmentId) => requestAdmin(`${PATH}/records/${id}/adjustments/${adjustmentId}/cancel`, { method: "POST" }),
  recordPdf: (id) => requestAdminBlob(`${PATH}/records/${id}/payslip.pdf`),

  report: (kind, params = {}, options = {}) => requestAdmin(`${PATH}/reports/${kind}${qs(params)}`, options),
  exportReports: (params = {}) => requestAdminBlob(`${PATH}/reports/export${qs(params)}`),
};

/** Phiếu lương cá nhân. */
export const payslipApi = {
  list: (options = {}) => requestAdmin("/api/payslips", options),
  detail: (id, options = {}) => requestAdmin(`/api/payslips/${id}`, options),
  pdf: (id) => requestAdminBlob(`/api/payslips/${id}/pdf`),
};
