import { requestAdmin } from "../http/adminSession.js";

const BASE = "/api/admin/refunds";

/** Khoản hoàn tiền từ thanh toán online (phạm vi cửa hàng kiểm tra ở backend). */
export const refundApi = {
  list: ({ status, page, size }, options = {}) => {
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    if (status) query.set("status", status);
    return requestAdmin(`${BASE}?${query}`, options);
  },
  complete: (id, transactionCode) => requestAdmin(`${BASE}/${id}/complete`, { method: "POST", body: { transactionCode } }),
  reject: (id, reason) => requestAdmin(`${BASE}/${id}/reject`, { method: "POST", body: { reason } }),
};
