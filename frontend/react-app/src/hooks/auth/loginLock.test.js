import test from "node:test";
import assert from "node:assert/strict";
import { formatLoginLock, getLoginRetrySeconds } from "./loginLock.js";

test("getLoginRetrySeconds accepts Retry-After only for a 429 login response", () => {
  assert.equal(getLoginRetrySeconds({ status: 429, retryAfterSeconds: "60" }), 60);
  assert.equal(getLoginRetrySeconds({ status: 401, retryAfterSeconds: "60" }), 0);
  assert.equal(getLoginRetrySeconds({ status: 429, retryAfterSeconds: "invalid" }), 0);
});

test("formatLoginLock renders a stable minute and second countdown", () => {
  assert.equal(formatLoginLock(60), "01:00");
  assert.equal(formatLoginLock(5), "00:05");
});
