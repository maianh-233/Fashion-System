import { getCustomerProfile, requestCustomer } from "./customerSession.js";

const ME = "/api/customers/me";

/** Hồ sơ của khách đang đăng nhập. */
export const profileApi = {
  get: (options = {}) => requestCustomer(`${ME}/profile`, options),
  update: (body) => requestCustomer(`${ME}/profile`, { method: "PUT", body }),
  changePassword: (body) => requestCustomer(`${ME}/password`, { method: "POST", body }),
};

/** Thông báo của khách (đơn hàng, liên hệ từ cửa hàng). */
export const customerNotificationApi = {
  list: (page = 0, size = 10, options = {}) => requestCustomer(`/api/notifications?page=${page}&size=${size}`, options),
  unreadCount: (options = {}) => requestCustomer("/api/notifications/unread-count", options),
  markRead: (id) => requestCustomer(`/api/notifications/${id}/read`, { method: "PATCH" }),
  markAllRead: () => requestCustomer("/api/notifications/read-all", { method: "PATCH" }),
};

/** Giỏ hàng của khách đã đăng nhập. */
export const cartApi = {
  get: (options = {}) => requestCustomer(`${ME}/cart`, options),
  add: (variantId, quantity) => requestCustomer(`${ME}/cart/items`, { method: "POST", body: { variantId, quantity } }),
  update: (variantId, patch) => requestCustomer(`${ME}/cart/items/${variantId}`, { method: "PATCH", body: patch }),
  remove: (variantId) => requestCustomer(`${ME}/cart/items/${variantId}`, { method: "DELETE" }),
  selectAll: (selected) => requestCustomer(`${ME}/cart/selection`, { method: "PUT", body: { selected } }),
  merge: (items) => requestCustomer(`${ME}/cart/merge`, { method: "POST", body: { items } }),
};

const addressPath = () => {
  const customer = getCustomerProfile();
  if (!customer?.id) throw new Error("Vui lòng đăng nhập để quản lý địa chỉ.");
  return `/api/customers/${customer.id}/addresses`;
};

/** Sổ địa chỉ của khách. */
export const addressApi = {
  list: (options = {}) => requestCustomer(`${addressPath()}?size=50&sort=createdAt,desc`, options),
  create: (body) => requestCustomer(addressPath(), { method: "POST", body }),
  update: (id, body) => requestCustomer(`${addressPath()}/${id}`, { method: "PUT", body }),
  remove: (id) => requestCustomer(`${addressPath()}/${id}`, { method: "DELETE" }),
  setDefault: (id) => requestCustomer(`${addressPath()}/${id}/default`, { method: "PUT" }),
};

/** Đặt hàng, đơn của tôi, thanh toán. */
export const customerOrderApi = {
  quote: (body, options = {}) => requestCustomer(`${ME}/orders/quote`, { ...options, method: "POST", body }),
  vouchers: (body, options = {}) => requestCustomer(`${ME}/orders/vouchers`, { ...options, method: "POST", body }),
  place: (body) => requestCustomer(`${ME}/orders`, { method: "POST", body }),
  list: ({ status, page = 0, size = 6 } = {}, options = {}) => {
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    if (status) query.set("status", status);
    return requestCustomer(`${ME}/orders?${query}`, options);
  },
  detail: (id, options = {}) => requestCustomer(`${ME}/orders/${id}`, options),
  cancelReasons: (options = {}) => requestCustomer(`${ME}/orders/cancel-reasons`, options),
  cancel: (id, body) => requestCustomer(`${ME}/orders/${id}/cancel`, { method: "POST", body }),
  pay: (id, method) => requestCustomer(`${ME}/orders/${id}/payments`, { method: "POST", body: method ? { method } : {} }),
  switchToCod: (id) => requestCustomer(`${ME}/orders/${id}/payment-method/cod`, { method: "POST" }),
};

/** Hỗ trợ đơn hàng (bot + nhân viên). */
export const supportChatApi = {
  open: (orderId) => requestCustomer(`${ME}/support/orders/${orderId}`, { method: "POST" }),
  conversation: (roomId, options = {}) => requestCustomer(`${ME}/support/rooms/${roomId}`, options),
  send: (roomId, body) => requestCustomer(`${ME}/support/rooms/${roomId}/messages`, { method: "POST", body }),
};
