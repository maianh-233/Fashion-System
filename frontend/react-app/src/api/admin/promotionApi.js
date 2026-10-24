import { requestAdmin } from "../http/adminSession.js";
import { queryString } from "../http/query.js";

const PATH = "/api/promotions";

export const promotionApi = {
  list: (params, options = {}) => requestAdmin(`${PATH}${queryString(params)}`, options),
  detail: (id, options = {}) => requestAdmin(`${PATH}/${id}`, options),
  byCode: (code, options = {}) => requestAdmin(`${PATH}/code/${encodeURIComponent(code)}`, options),
  create: (body) => requestAdmin(PATH, { method: "POST", body }),
  update: (id, body) => requestAdmin(`${PATH}/${id}`, { method: "PUT", body }),
  remove: (id) => requestAdmin(`${PATH}/${id}`, { method: "DELETE" }),
  restore: (id) => requestAdmin(`${PATH}/${id}/restore`, { method: "PATCH" }),
};

export const customerTierOptionsApi = {
  list: (options = {}) => requestAdmin(`/api/customer-tiers${queryString({ size: 100 })}`, options),
};
