export const EMPTY_REVIEW_FILTERS = { keyword: "", rating: "", visibility: "" };

/** Tham số cho reviewAdminApi.list từ bộ lọc trên màn hình ({@code page} bắt đầu từ 1). */
export function reviewQuery(filters, page, size) {
  return {
    keyword: filters.keyword?.trim() || undefined,
    rating: filters.rating ? Number(filters.rating) : undefined,
    hidden: filters.visibility === "hidden" ? true : filters.visibility === "visible" ? false : undefined,
    page: Math.max(0, page - 1),
    size,
  };
}

/** Trang Spring ({content, page:{totalPages,totalElements}}) → dữ liệu bảng. */
export function normalizeReviewPage(response) {
  const meta = response?.page ?? response ?? {};
  return {
    rows: Array.isArray(response?.content) ? response.content : [],
    totalPages: Math.max(1, Number(meta.totalPages || 1)),
    totalElements: Number(meta.totalElements || 0),
  };
}
