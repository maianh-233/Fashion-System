import { requestAdmin } from "../http/adminSession.js";

const PATH = "/api/admin/reviews";

/** Quản trị đánh giá sản phẩm: danh sách (kể cả đã ẩn), ẩn/hiện có lý do, trả lời khách. */
export const reviewAdminApi = {
  list: ({ keyword, rating, hidden, page = 0, size = 20 } = {}, options = {}) => {
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    if (keyword?.trim()) query.set("keyword", keyword.trim());
    if (rating) query.set("rating", String(rating));
    if (hidden === true || hidden === false) query.set("hidden", String(hidden));
    return requestAdmin(`${PATH}?${query}`, options);
  },
  setVisibility: (id, hidden, reason) =>
    requestAdmin(`${PATH}/${id}/visibility`, { method: "PATCH", body: { hidden, reason: reason || null } }),
  reply: (id, reply) => requestAdmin(`${PATH}/${id}/reply`, { method: "PUT", body: { reply: reply || null } }),
};
