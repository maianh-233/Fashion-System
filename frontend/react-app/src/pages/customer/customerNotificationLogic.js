// Logic thuần của chuông thông báo phía khách.

/** Thêm thông báo mới (realtime) lên đầu, bỏ trùng theo id, giữ tối đa {@code limit} mục. */
export function prependNotification(list = [], notification, limit = 10) {
  if (!notification?.id) return list;
  return [notification, ...list.filter((item) => item.id !== notification.id)].slice(0, limit);
}

/** Đánh dấu đã đọc trong danh sách đang hiển thị. */
export function markReadLocally(list = [], id) {
  return list.map((item) => (id == null || item.id === id ? { ...item, read: true } : item));
}

/** Đường dẫn nội bộ an toàn để mở từ thông báo; link ngoài hoặc lạ thì về trang đơn hàng. */
export function notificationTarget(notification) {
  const url = notification?.actionUrl;
  if (typeof url === "string" && url.startsWith("/") && !url.startsWith("//") && !url.startsWith("/admin")) return url;
  if (notification?.entityType === "ORDER" && notification.entityId) return `/orderdetail?id=${notification.entityId}`;
  return "/orders";
}

/** "vừa xong", "5 phút trước", "3 giờ trước", hoặc ngày. */
export function relativeTime(value, now = Date.now()) {
  if (!value) return "";
  const diff = Math.max(0, now - new Date(value).getTime());
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return "vừa xong";
  if (minutes < 60) return `${minutes} phút trước`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} giờ trước`;
  return new Date(value).toLocaleDateString("vi-VN");
}

/** Số hiển thị trên huy hiệu chuông. */
export const badgeLabel = (count) => (count > 99 ? "99+" : String(count));
