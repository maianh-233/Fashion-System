import { requestAdmin } from "../http/adminSession.js";

export function getNotifications(page = 0, size = 20, options = {}) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  return requestAdmin(`/api/notifications?${query}`, options);
}

export function getUnreadNotificationCount(options = {}) {
  return requestAdmin("/api/notifications/unread-count", options);
}

export function markNotificationRead(id) {
  return requestAdmin(`/api/notifications/${id}/read`, { method: "PATCH" });
}

export function markAllNotificationsRead() {
  return requestAdmin("/api/notifications/read-all", { method: "PATCH" });
}
