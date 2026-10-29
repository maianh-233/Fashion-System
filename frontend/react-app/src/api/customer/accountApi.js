import { getCustomerProfile, requestCustomer } from "../http/customerSession.js";
import { measurementQuery } from "../../pages/customer/sizeGuideLogic.js";

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

/** Sản phẩm yêu thích của khách đã đăng nhập: {productIds}. */
export const wishlistApi = {
  get: (options = {}) => requestCustomer(`${ME}/wishlist`, options),
  add: (productId) => requestCustomer(`${ME}/wishlist/${productId}`, { method: "PUT" }),
  remove: (productId) => requestCustomer(`${ME}/wishlist/${productId}`, { method: "DELETE" }),
  merge: (productIds) => requestCustomer(`${ME}/wishlist/merge`, { method: "POST", body: { productIds } }),
};

/** Lịch sử điểm tích lũy (tích theo đơn, trừ khi hoàn tiền, đổi voucher). */
export const loyaltyApi = {
  transactions: ({ page = 0, size = 10 } = {}, options = {}) =>
    requestCustomer(`${ME}/loyalty/transactions?page=${page}&size=${size}`, options),
};

/** Ví voucher: voucher đổi được bằng điểm và voucher khách đã đổi. */
export const voucherWalletApi = {
  redeemable: ({ page = 0, size = 9 } = {}, options = {}) =>
    requestCustomer(`${ME}/promotions/redeemable?page=${page}&size=${size}`, options),
  mine: ({ page = 0, size = 9 } = {}, options = {}) => requestCustomer(`${ME}/promotions?page=${page}&size=${size}`, options),
  redeem: (promotionId) => requestCustomer(`${ME}/promotions/${promotionId}/redeem`, { method: "POST" }),
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

/** Đánh giá sản phẩm của khách: sản phẩm chờ đánh giá (đơn đã hoàn thành) và đánh giá đã viết. */
export const reviewApi = {
  pending: (options = {}) => requestCustomer(`${ME}/reviews/pending`, options),
  mine: (options = {}) => requestCustomer(`${ME}/reviews`, options),
  /** {orderId, productId, rating, content, images: File[] (tối đa 3)} gửi dạng multipart. */
  create: ({ orderId, productId, rating, content, images = [] }) => {
    const body = new FormData();
    body.append("orderId", orderId);
    body.append("productId", productId);
    body.append("rating", String(rating));
    if (content?.trim()) body.append("content", content.trim());
    images.forEach((file) => body.append("images", file));
    return requestCustomer(`${ME}/reviews`, { method: "POST", body });
  },
};

/** "Báo khi có hàng" theo biến thể; subscribe/unsubscribe trả {variantIds} khách đang chờ của sản phẩm đó. */
export const stockAlertApi = {
  mine: (options = {}) => requestCustomer(`${ME}/stock-alerts`, options),
  pending: (productId, options = {}) =>
    requestCustomer(`${ME}/stock-alerts/pending?productId=${encodeURIComponent(productId)}`, options),
  subscribe: (variantId) => requestCustomer(`${ME}/stock-alerts/${variantId}`, { method: "PUT" }),
  unsubscribe: (variantId) => requestCustomer(`${ME}/stock-alerts/${variantId}`, { method: "DELETE" }),
};

/** Số đo của khách và gợi ý size có xét size khách đã mua và giữ lại. */
export const sizeProfileApi = {
  measurements: (options = {}) => requestCustomer(`${ME}/measurements`, options),
  saveMeasurements: (body) => requestCustomer(`${ME}/measurements`, { method: "PUT", body }),
  guide: (productId, { heightCm, weightKg } = {}, options = {}) =>
    requestCustomer(`${ME}/size-guide?productId=${encodeURIComponent(productId)}${measurementQuery(heightCm, weightKg, "&")}`, options),
};

/** Đổi / trả hàng cho đơn đã hoàn thành (14 ngày). */
export const returnApi = {
  eligibility: (orderId, options = {}) =>
    requestCustomer(`${ME}/returns/eligibility?orderId=${encodeURIComponent(orderId)}`, options),
  list: (orderId, options = {}) =>
    requestCustomer(`${ME}/returns${orderId ? `?orderId=${encodeURIComponent(orderId)}` : ""}`, options),
  create: (body) => requestCustomer(`${ME}/returns`, { method: "POST", body }),
  cancel: (id) => requestCustomer(`${ME}/returns/${id}/cancel`, { method: "POST" }),
};
