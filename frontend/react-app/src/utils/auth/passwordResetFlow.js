const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function buildEmployeeOtpPayload(value) {
  const email = String(value || "").trim().toLowerCase();
  if (!email || email.length > 255 || !EMAIL_PATTERN.test(email)) {
    throw new Error("Vui lòng nhập địa chỉ email nhân viên hợp lệ.");
  }
  return { email, accountType: "EMPLOYEE" };
}

export function normalizeOtp(value) {
  const otp = String(value || "").trim();
  if (!/^\d{6}$/.test(otp)) throw new Error("Mã OTP phải gồm đúng 6 chữ số.");
  return otp;
}

export function validateNewPassword(passwordValue, confirmationValue) {
  const password = String(passwordValue || "");
  const confirmation = String(confirmationValue || "");
  if (password.length < 8 || password.length > 72) {
    throw new Error("Mật khẩu mới phải có từ 8 ký tự đến 72 ký tự.");
  }
  if (password !== confirmation) throw new Error("Mật khẩu xác nhận không khớp.");
  return password;
}
