import { requestAdmin } from "./auth/adminSession.js";
import { buildCatalogQuery } from "../utils/catalogQuery.js";

const PATH = "/api/admin/orders";

/** API quản trị đơn hàng. Cửa hàng hiệu lực luôn được backend kiểm tra lại theo phạm vi nhân viên. */
export const orderApi = {
  meta: (options = {}) => requestAdmin(`${PATH}/meta`, options),
  statusHistory: (id, options = {}) => requestAdmin(`${PATH}/${id}/status-history`, options),
  list: (params, options = {}) => requestAdmin(`${PATH}${buildCatalogQuery(params)}`, options),
  summary: (params, options = {}) => requestAdmin(`${PATH}/summary${buildCatalogQuery(params)}`, options),
  handlers: (storeId, options = {}) => requestAdmin(`${PATH}/handlers${buildCatalogQuery({ storeId })}`, options),
  detail: (id, options = {}) => requestAdmin(`${PATH}/${id}`, options),
  hide: (id) => requestAdmin(`${PATH}/${id}/hide`, { method: "POST" }),
  restore: (id) => requestAdmin(`${PATH}/${id}/restore`, { method: "POST" }),
  confirm: (id) => requestAdmin(`${PATH}/${id}/confirm`, { method: "POST" }),
  cancel: (id, reason) => requestAdmin(`${PATH}/${id}/cancel`, { method: "POST", body: { reason } }),
  confirmReturn: (id) => requestAdmin(`${PATH}/${id}/confirm-return`, { method: "POST" }),
  handover: (id) => requestAdmin(`${PATH}/${id}/handover`, { method: "POST" }),
  contacts: (id, options = {}) => requestAdmin(`${PATH}/${id}/contacts`, options),
  contact: (id, body) => requestAdmin(`${PATH}/${id}/contacts`, { method: "POST", body }),

  variantByCode: (storeId, code, options = {}) =>
    requestAdmin(`${PATH}/offline/variants/by-code${buildCatalogQuery({ storeId, code })}`, options),
  products: (params, options = {}) => requestAdmin(`${PATH}/offline/products${buildCatalogQuery(params)}`, options),
  productVariants: (productId, storeId, options = {}) =>
    requestAdmin(`${PATH}/offline/products/${productId}/variants${buildCatalogQuery({ storeId })}`, options),
  vouchers: (body, options = {}) => requestAdmin(`${PATH}/offline/vouchers`, { ...options, method: "POST", body }),
  quote: (body, options = {}) => requestAdmin(`${PATH}/offline/quote`, { ...options, method: "POST", body }),
  createOffline: (body) => requestAdmin(`${PATH}/offline`, { method: "POST", body }),
};

export const customerLookupApi = {
  /** Tìm khách theo SĐT/email/mã khách; 404 khi không có. */
  lookup: (value, options = {}) => requestAdmin(`/api/customers/lookup${buildCatalogQuery({ value })}`, options),
};
