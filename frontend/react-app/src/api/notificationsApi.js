import { requestAdmin } from "./auth/adminSession";

export function getNotifications(page = 0, size = 20, options = {}) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  return requestAdmin(`/api/notifications?${query}`, options);
}
