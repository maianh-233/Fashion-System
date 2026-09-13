import test from "node:test";
import assert from "node:assert/strict";
import {
  buildEmployeeOtpPayload,
  normalizeOtp,
  validateNewPassword,
} from "./passwordResetFlow.js";

test("admin password reset always targets the EMPLOYEE account type", () => {
  assert.deepEqual(buildEmployeeOtpPayload("  Admin@Lunaria.VN  "), {
    email: "admin@lunaria.vn",
    accountType: "EMPLOYEE",
  });
});

test("normalizeOtp accepts exactly six digits and rejects other user input", () => {
  assert.equal(normalizeOtp(" 123456 "), "123456");
  assert.throws(() => normalizeOtp("12345a"), /6 chữ số/);
});

test("validateNewPassword requires matching passwords between 8 and 72 characters", () => {
  assert.equal(validateNewPassword("MatKhauMoi123", "MatKhauMoi123"), "MatKhauMoi123");
  assert.throws(() => validateNewPassword("short", "short"), /8 ký tự/);
  assert.throws(() => validateNewPassword("MatKhauMoi123", "KhongTrung123"), /không khớp/);
});
