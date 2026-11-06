import { requestAdmin, requestAdminBlob } from "../http/adminSession.js";

const ADMIN = "/api/admin/attendance";
const SELF = "/api/attendance";

function qs(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    query.set(key, String(value));
  });
  const text = query.toString();
  return text ? `?${text}` : "";
}

/** Chấm công cá nhân (mọi nhân viên). */
export const myAttendanceApi = {
  today: (options = {}) => requestAdmin(`${SELF}/today`, options),
  checkIn: (body) => requestAdmin(`${SELF}/check-in`, { method: "POST", body }),
  checkOut: (body) => requestAdmin(`${SELF}/check-out`, { method: "POST", body }),
  history: (params, options = {}) => requestAdmin(`${SELF}/history${qs(params)}`, options),
  leaveTypes: (options = {}) => requestAdmin(`${SELF}/leave-types`, options),
  leaveBalance: (year, options = {}) => requestAdmin(`${SELF}/leave-balance${qs({ year })}`, options),
  excuses: (options = {}) => requestAdmin(`${SELF}/excuses`, options),
  createExcuse: (body) => requestAdmin(`${SELF}/excuses`, { method: "POST", body }),
  cancelExcuse: (id) => requestAdmin(`${SELF}/excuses/${id}/cancel`, { method: "POST" }),
  leaves: (options = {}) => requestAdmin(`${SELF}/leaves`, options),
  createLeave: (body) => requestAdmin(`${SELF}/leaves`, { method: "POST", body }),
  cancelLeave: (id) => requestAdmin(`${SELF}/leaves/${id}/cancel`, { method: "POST" }),
  overtime: (options = {}) => requestAdmin(`${SELF}/overtime`, options),
  createOvertime: (body) => requestAdmin(`${SELF}/overtime`, { method: "POST", body }),
  cancelOvertime: (id) => requestAdmin(`${SELF}/overtime/${id}/cancel`, { method: "POST" }),
};

/** Quản lý chấm công (HR / quản lý cửa hàng; phạm vi kiểm tra ở backend). */
export const attendanceApi = {
  lookups: (options = {}) => requestAdmin(`${ADMIN}/lookups`, options),
  dashboard: (params = {}, options = {}) => requestAdmin(`${ADMIN}/dashboard${qs(params)}`, options),
  employees: (params = {}, options = {}) => requestAdmin(`${ADMIN}/employees${qs(params)}`, options),
  records: (params = {}, options = {}) => requestAdmin(`${ADMIN}/records${qs(params)}`, options),
  record: (id, options = {}) => requestAdmin(`${ADMIN}/records/${id}`, options),
  adjust: (id, body) => requestAdmin(`${ADMIN}/records/${id}`, { method: "PUT", body }),
  createRecord: (body) => requestAdmin(`${ADMIN}/records`, { method: "POST", body }),
  exportRecords: (params = {}) => requestAdminBlob(`${ADMIN}/export${qs(params)}`),

  requests: (kind, params = {}, options = {}) => requestAdmin(`${ADMIN}/${kind}${qs(params)}`, options),
  approve: (kind, id, body = {}) => requestAdmin(`${ADMIN}/${kind}/${id}/approve`, { method: "POST", body }),
  reject: (kind, id, reason) => requestAdmin(`${ADMIN}/${kind}/${id}/reject`, { method: "POST", body: { reason } }),

  settings: (options = {}) => requestAdmin(`${ADMIN}/settings`, options),
  updateSettings: (body) => requestAdmin(`${ADMIN}/settings`, { method: "PUT", body }),
  holidays: (year, options = {}) => requestAdmin(`${ADMIN}/holidays${qs({ year })}`, options),
  addHoliday: (body) => requestAdmin(`${ADMIN}/holidays`, { method: "POST", body }),
  deleteHoliday: (date) => requestAdmin(`${ADMIN}/holidays/${date}`, { method: "DELETE" }),
  createLeaveType: (body) => requestAdmin(`${ADMIN}/leave-types`, { method: "POST", body }),
  updateLeaveType: (id, body) => requestAdmin(`${ADMIN}/leave-types/${id}`, { method: "PUT", body }),
};

/** Lưu file trả về từ requestAdminBlob. */
export function saveBlob({ blob, contentDisposition }, fallbackName) {
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(contentDisposition || "");
  const name = match ? decodeURIComponent(match[1]) : fallbackName;
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = name;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
