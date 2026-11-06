import { requestAdmin } from "../http/adminSession.js";

const ADMIN = "/api/admin/shifts";
const SELF = "/api/shifts";

function qs(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    query.set(key, String(value));
  });
  const text = query.toString();
  return text ? `?${text}` : "";
}

/** Xếp ca (quản lý cửa hàng / HR; phạm vi cửa hàng kiểm tra ở backend). */
export const shiftApi = {
  templates: (params = {}, options = {}) => requestAdmin(`${ADMIN}/templates${qs(params)}`, options),
  createTemplate: (body) => requestAdmin(`${ADMIN}/templates`, { method: "POST", body }),
  updateTemplate: (id, body) => requestAdmin(`${ADMIN}/templates/${id}`, { method: "PUT", body }),

  schedule: (params = {}, options = {}) => requestAdmin(`${ADMIN}/schedule${qs(params)}`, options),
  createEntry: (body) => requestAdmin(`${ADMIN}/schedule`, { method: "POST", body }),
  updateEntry: (id, body) => requestAdmin(`${ADMIN}/schedule/${id}`, { method: "PUT", body }),
  deleteEntry: (id) => requestAdmin(`${ADMIN}/schedule/${id}`, { method: "DELETE" }),
  publish: (body) => requestAdmin(`${ADMIN}/schedule/publish`, { method: "POST", body }),
  copyWeek: (body) => requestAdmin(`${ADMIN}/schedule/copy-week`, { method: "POST", body }),

  swaps: (params = {}, options = {}) => requestAdmin(`${ADMIN}/swaps${qs(params)}`, options),
  approveSwap: (id, note) => requestAdmin(`${ADMIN}/swaps/${id}/approve`, { method: "POST", body: { note } }),
  rejectSwap: (id, reason) => requestAdmin(`${ADMIN}/swaps/${id}/reject`, { method: "POST", body: { reason } }),
};

/** Lịch làm cá nhân (mọi nhân viên). */
export const myShiftApi = {
  mine: (params, options = {}) => requestAdmin(`${SELF}/mine${qs(params)}`, options),
  availability: (params, options = {}) => requestAdmin(`${SELF}/availability${qs(params)}`, options),
  setAvailability: (body) => requestAdmin(`${SELF}/availability`, { method: "PUT", body }),
  clearAvailability: (date) => requestAdmin(`${SELF}/availability/${date}`, { method: "DELETE" }),
  colleagues: (scheduleId, options = {}) => requestAdmin(`${SELF}/${scheduleId}/colleagues`, options),
  swaps: (options = {}) => requestAdmin(`${SELF}/swaps`, options),
  createSwap: (body) => requestAdmin(`${SELF}/swaps`, { method: "POST", body }),
  accept: (id, note) => requestAdmin(`${SELF}/swaps/${id}/accept`, { method: "POST", body: { note } }),
  decline: (id, note) => requestAdmin(`${SELF}/swaps/${id}/decline`, { method: "POST", body: { note } }),
  cancel: (id) => requestAdmin(`${SELF}/swaps/${id}/cancel`, { method: "POST" }),
};
