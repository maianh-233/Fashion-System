import { requestAdmin } from "../http/adminSession.js";

const PATH = "/api/admin/transfers";

const pageQuery = (params) => {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") query.set(key, String(value));
  });
  return query;
};

/** Điều chuyển hàng giữa cửa hàng và hàng sắp hết (storeId bỏ trống: cửa hàng của nhân viên). */
export const transferApi = {
  list: ({ storeId, direction, status, page = 0, size = 20 } = {}, options = {}) =>
    requestAdmin(`${PATH}?${pageQuery({ storeId, direction, status, page, size })}`, options),
  lowStock: ({ storeId, keyword, page = 0, size = 20 } = {}, options = {}) =>
    requestAdmin(`${PATH}/low-stock?${pageQuery({ storeId, keyword, page, size })}`, options),
  detail: (id, options = {}) => requestAdmin(`${PATH}/${id}`, options),
  create: (body) => requestAdmin(PATH, { method: "POST", body }),
  cancel: (id) => requestAdmin(`${PATH}/${id}/cancel`, { method: "POST" }),
  reject: (id, reason) => requestAdmin(`${PATH}/${id}/reject`, { method: "POST", body: { reason } }),
  /** quantities: {variantId: số xuất}; bỏ trống là xuất đủ số yêu cầu. */
  ship: (id, quantities) => requestAdmin(`${PATH}/${id}/ship`, { method: "POST", body: { quantities } }),
  receive: (id) => requestAdmin(`${PATH}/${id}/receive`, { method: "POST" }),
};
