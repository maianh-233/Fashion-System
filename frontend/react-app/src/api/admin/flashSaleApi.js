import { requestAdmin } from "../http/adminSession.js";

const PATH = "/api/admin/flash-sales";

/** Quản lý flash sale (quyền khuyến mãi). */
export const flashSaleApi = {
  list: ({ page = 0, size = 20 } = {}, options = {}) => requestAdmin(`${PATH}?page=${page}&size=${size}`, options),
  get: (id, options = {}) => requestAdmin(`${PATH}/${id}`, options),
  create: (body) => requestAdmin(PATH, { method: "POST", body }),
  update: (id, body) => requestAdmin(`${PATH}/${id}`, { method: "PUT", body }),
  setActive: (id, value) => requestAdmin(`${PATH}/${id}/active?value=${value}`, { method: "PATCH" }),
};
