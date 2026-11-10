import { requestPublic } from "../http/publicRequest.js";

const PATH = "/api/public/marketing/unsubscribe";

/** Hủy nhận email khuyến mãi từ link trong email (xác thực bằng token, không cần đăng nhập). */
export const marketingUnsubscribeApi = {
  info: (token, options = {}) => requestPublic(`${PATH}?token=${encodeURIComponent(token)}`, options),
  unsubscribe: (token) => requestPublic(PATH, { method: "POST", body: { token } }),
};
