import { requestAdmin } from "../http/adminSession.js";

/** Hồ sơ của nhân viên đang đăng nhập. */
export const getAdminProfile = (options = {}) => requestAdmin("/api/users/me", options);
export const updateAdminProfile = (body) => requestAdmin("/api/users/me", { method: "PATCH", body });
export const changeAdminPassword = (body) => requestAdmin("/api/users/me/password", { method: "POST", body });
