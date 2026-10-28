export const REVIEW_PAGE_SIZE = 5;
export const REVIEW_MAX_IMAGES = 3;
export const REVIEW_MAX_CONTENT = 2000;
export const REVIEW_MAX_IMAGE_BYTES = 5 * 1024 * 1024;
const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];

export const RATING_LABELS = { 1: "Rất tệ", 2: "Tệ", 3: "Bình thường", 4: "Hài lòng", 5: "Tuyệt vời" };

/**
 * Ghép một trang đánh giá ({content, page:{number,totalElements}}) vào danh sách đang hiển thị.
 * Trang của bộ lọc khác ({@code key} khác) thay thế danh sách thay vì nối thêm.
 */
export function mergeReviewPage(current, page, key) {
  const items = Array.isArray(page?.content) ? page.content : [];
  const sameList = current?.key === key;
  const known = new Set(sameList ? current.items.map((review) => review.id) : []);
  return {
    key,
    items: [...(sameList ? current.items : []), ...items.filter((review) => !known.has(review.id))],
    total: Number(page?.page?.totalElements ?? page?.totalElements ?? 0),
    page: Number(page?.page?.number ?? page?.number ?? 0),
    error: "",
  };
}

/** Tỷ lệ % (làm tròn) của từng mức 5→1 sao trên tổng số đánh giá. */
export function starPercents(summary) {
  const stars = Array.isArray(summary?.stars) ? summary.stars : [0, 0, 0, 0, 0];
  const total = Number(summary?.count || 0);
  return stars.map((count) => (total > 0 ? Math.round((Number(count) / total) * 100) : 0));
}

/** Lỗi của form đánh giá (rỗng nếu hợp lệ); kiểm tra trước khi gửi để khỏi tải ảnh lên vô ích. */
export function validateReviewForm({ rating, content = "", images = [] }) {
  const errors = {};
  if (!Number.isInteger(rating) || rating < 1 || rating > 5) errors.rating = "Vui lòng chọn số sao.";
  if (content.trim().length > REVIEW_MAX_CONTENT) errors.content = `Nhận xét tối đa ${REVIEW_MAX_CONTENT} ký tự.`;
  if (images.length > REVIEW_MAX_IMAGES) errors.images = `Tối đa ${REVIEW_MAX_IMAGES} ảnh.`;
  else if (images.some((file) => !IMAGE_TYPES.includes(file.type))) errors.images = "Chỉ nhận ảnh JPG, PNG hoặc WEBP.";
  else if (images.some((file) => file.size > REVIEW_MAX_IMAGE_BYTES)) errors.images = "Mỗi ảnh tối đa 5 MB.";
  return errors;
}

/** Thêm ảnh mới vào danh sách đã chọn, bỏ trùng (cùng tên + kích thước) và cắt ở giới hạn. */
export function addReviewImages(current, incoming) {
  const seen = new Set(current.map((file) => `${file.name}:${file.size}`));
  const fresh = Array.from(incoming || []).filter((file) => !seen.has(`${file.name}:${file.size}`));
  return [...current, ...fresh].slice(0, REVIEW_MAX_IMAGES);
}
