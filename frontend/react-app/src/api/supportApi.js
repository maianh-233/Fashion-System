import { requestAdmin } from "./auth/adminSession.js";

const PATH = "/api/admin/support";

/** Hội thoại hỗ trợ đơn hàng phía nhân viên và câu trả lời của bot. */
export const supportApi = {
  rooms: ({ storeId, statuses = [], page = 0, size = 30 }, options = {}) => {
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    if (storeId) query.set("storeId", storeId);
    statuses.forEach(status => query.append("status", status));
    return requestAdmin(`${PATH}/rooms?${query}`, options);
  },
  conversation: (roomId, options = {}) => requestAdmin(`${PATH}/rooms/${roomId}`, options),
  claim: (roomId) => requestAdmin(`${PATH}/rooms/${roomId}/claim`, { method: "POST" }),
  reply: (roomId, text) => requestAdmin(`${PATH}/rooms/${roomId}/messages`, { method: "POST", body: { text } }),
  close: (roomId) => requestAdmin(`${PATH}/rooms/${roomId}/close`, { method: "POST" }),
  faqs: (options = {}) => requestAdmin(`${PATH}/faqs`, options),
  updateFaq: (id, body) => requestAdmin(`${PATH}/faqs/${id}`, { method: "PUT", body }),
};
