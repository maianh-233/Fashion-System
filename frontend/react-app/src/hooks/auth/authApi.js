const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");
const AUTH_API_URL = `${API_BASE_URL}/api/auth`;

/** Các loại tài khoản backend chấp nhận trong luồng quên mật khẩu. */
export const PASSWORD_RESET_ACCOUNT_TYPES = Object.freeze({
  EMPLOYEE: "EMPLOYEE",
  CUSTOMER: "CUSTOMER",
});

/** Các nhà cung cấp đăng nhập mạng xã hội được backend hỗ trợ. */
export const SOCIAL_PROVIDERS = Object.freeze({
  GOOGLE: "GOOGLE",
  FACEBOOK: "FACEBOOK",
});

/** Lỗi HTTP từ auth API, giữ lại status và response body để giao diện tự xử lý. */
export class AuthApiError extends Error {
  /** Khởi tạo lỗi auth từ thông tin phản hồi của backend. */
  constructor(message, status, data) {
    super(message);
    this.name = "AuthApiError";
    this.status = status;
    this.data = data;
  }
}

/** Đọc response body dưới dạng JSON hoặc text tùy content type của backend. */
async function parseResponseBody(response) {
  if (response.status === 204) return null;

  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) return response.json();

  const text = await response.text();
  return text || null;
}

/** Lấy thông báo lỗi dễ dùng nhất từ response lỗi của Spring Boot. */
function getErrorMessage(data, status) {
  if (typeof data === "string" && data) return data;

  return (
    data?.message ||
    data?.detail ||
    data?.error ||
    `Yêu cầu xác thực thất bại (${status})`
  );
}

/** Gửi request chung đến auth API và chuẩn hóa dữ liệu hoặc lỗi trả về. */
async function requestAuth(path, { body, token, signal } = {}) {
  const headers = { Accept: "application/json" };

  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch(`${AUTH_API_URL}${path}`, {
    method: "POST",
    credentials: "include",
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  });
  const data = await parseResponseBody(response);

  if (!response.ok) {
    throw new AuthApiError(getErrorMessage(data, response.status), response.status, data);
  }

  return data;
}

/** Đăng ký tài khoản khách hàng và trả về JWT cùng thông tin khách hàng. */
export function registerCustomer(payload, options = {}) {
  return requestAuth("/register/customer", { ...options, body: payload });
}

/** Đăng nhập tài khoản khách hàng bằng username và password. */
export function loginCustomer(payload, options = {}) {
  return requestAuth("/login/customer", { ...options, body: payload });
}

/** Đăng ký hoặc đăng nhập khách hàng bằng token Google hoặc Facebook. */
export function loginSocialCustomer(payload, options = {}) {
  return requestAuth("/login/customer/social", { ...options, body: payload });
}

/** Tạo tài khoản nhân viên; options.token phải là JWT có quyền USER_CREATE. */
export function registerEmployee(payload, options = {}) {
  return requestAuth("/register/employee", { ...options, body: payload });
}

/** Tạo tài khoản admin; options.token phải là JWT có quyền USER_CREATE_ADMIN. */
export function registerAdmin(payload, options = {}) {
  return requestAuth("/register/admin", { ...options, body: payload });
}

/** Đăng nhập tài khoản nội bộ qua endpoint đăng nhập chung. */
export function login(payload, options = {}) {
  return requestAuth("/login", { ...options, body: payload });
}

/** Đăng nhập tài khoản nhân viên qua endpoint có tên rõ nghĩa. */
export function loginEmployee(payload, options = {}) {
  return requestAuth("/login/employee", { ...options, body: payload });
}

/** Gọi API đăng xuất bằng JWT; phía giao diện vẫn cần tự xóa token đã lưu. */
export function logout(token, options = {}) {
  return requestAuth("/logout", { ...options, token });
}

/** Yêu cầu backend gửi OTP quên mật khẩu đến email. */
export function requestPasswordResetOtp(payload, options = {}) {
  return requestAuth("/password/request-otp", { ...options, body: payload });
}

/** Yêu cầu backend gửi lại OTP quên mật khẩu đến email. */
export function resendPasswordResetOtp(payload, options = {}) {
  return requestAuth("/password/resend-otp", { ...options, body: payload });
}

/** Xác minh OTP và trả về reset token ngắn hạn để đổi mật khẩu. */
export function verifyPasswordResetOtp(payload, options = {}) {
  return requestAuth("/password/verify-otp", { ...options, body: payload });
}

/** Đặt mật khẩu mới bằng reset token đã nhận sau bước xác minh OTP. */
export function resetPassword(payload, options = {}) {
  return requestAuth("/password/reset", { ...options, body: payload });
}
