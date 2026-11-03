import { requestAdmin } from "../http/adminSession.js";

const PATH = "/api/admin/voucher-campaigns";

/** Chiến dịch voucher theo nhóm khách (quyền khuyến mãi, chỉ nhân viên cấp chuỗi được tạo / hủy). */
export const voucherCampaignApi = {
  list: ({ page = 0, size = 20 } = {}, options = {}) => requestAdmin(`${PATH}?page=${page}&size=${size}`, options),
  get: (id, options = {}) => requestAdmin(`${PATH}/${id}`, options),
  preview: (criteria, options = {}) => requestAdmin(`${PATH}/preview`, { method: "POST", body: criteria, ...options }),
  create: (body) => requestAdmin(PATH, { method: "POST", body }),
  cancel: (id) => requestAdmin(`${PATH}/${id}/cancel`, { method: "POST" }),
};
