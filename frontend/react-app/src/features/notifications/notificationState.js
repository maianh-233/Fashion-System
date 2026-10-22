export function isNotification(value) {
  return Boolean(value
    && typeof value === "object"
    && typeof value.id === "string"
    && typeof value.eventId === "string"
    && typeof value.type === "string"
    && typeof value.title === "string"
    && typeof value.message === "string"
    && typeof value.createdAt === "string");
}

export function mergeNotifications(current = [], incoming = []) {
  const byId = new Map();
  [...current, ...incoming].forEach((notification) => {
    if (isNotification(notification)) byId.set(notification.id, notification);
  });
  return [...byId.values()].sort((left, right) => {
    const timeDifference = Date.parse(right.createdAt) - Date.parse(left.createdAt);
    return timeDifference || right.id.localeCompare(left.id);
  });
}

export function notificationFromRealtime(event) {
  if (event?.channel !== "NOTIFICATION" || event?.eventType !== "NOTIFICATION_CREATED") return null;
  return isNotification(event.payload) ? event.payload : null;
}

/** Đánh dấu một thông báo đã đọc trong danh sách hiện có (cập nhật lạc quan trước khi API trả về). */
export function markItemRead(items = [], id) {
  return items.map((item) => (item.id === id ? { ...item, read: true } : item));
}

export function markAllItemsRead(items = []) {
  return items.map((item) => (item.read ? item : { ...item, read: true }));
}

/** Số hiển thị trên chuông: ẩn khi 0, rút gọn khi quá 99. */
export function unreadBadge(count) {
  const value = Math.max(0, Number(count) || 0);
  if (value === 0) return "";
  return value > 99 ? "99+" : String(value);
}
