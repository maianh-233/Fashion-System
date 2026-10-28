import { requestAdmin } from "../http/adminSession.js";

const PATH = "/api/admin/returns";

/** Yêu cầu đổi / trả của các cửa hàng nhân viên phụ trách. */
export const returnAdminApi = {
  list: ({ status, keyword, page = 0, size = 20 } = {}, options = {}) => {
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    if (status) query.set("status", status);
    if (keyword?.trim()) query.set("keyword", keyword.trim());
    return requestAdmin(`${PATH}?${query}`, options);
  },
  detail: (id, options = {}) => requestAdmin(`${PATH}/${id}`, options),
  approve: (id) => requestAdmin(`${PATH}/${id}/approve`, { method: "POST" }),
  reject: (id, reason) => requestAdmin(`${PATH}/${id}/reject`, { method: "POST", body: { reason } }),
  /** conditions: {itemId: "GOOD" | "DAMAGED"} */
  complete: (id, conditions) => requestAdmin(`${PATH}/${id}/complete`, { method: "POST", body: { conditions } }),
  confirmRefund: (id, transactionCode) =>
    requestAdmin(`${PATH}/${id}/confirm-refund`, { method: "POST", body: { transactionCode: transactionCode || null } }),
};
