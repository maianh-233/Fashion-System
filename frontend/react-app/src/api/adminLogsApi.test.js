import test from "node:test";
import assert from "node:assert/strict";

globalThis.window = {
  localStorage: { removeItem() {} },
  dispatchEvent() {},
};

const logsApi = await import("./adminLogsApi.js");

test("business log query maps every supported backend filter and pagination", () => {
  assert.equal(typeof logsApi.buildSystemLogQuery, "function");
  assert.deepEqual(logsApi.buildSystemLogQuery("business", {
    action: " UPDATE ", username: " lan ", actorUserId: " user-1 ",
    table: " users ", rowId: " row-1 ", requestId: " req-1 ",
    fromAt: "2026-10-01T08:00", toAt: "2026-10-06T18:00",
  }, 3), {
    page: 2, size: 25, action: "UPDATE", username: "lan", actorUserId: "user-1",
    table: "users", rowId: "row-1", requestId: "req-1",
    fromAt: "2026-10-01T08:00", toAt: "2026-10-06T18:00",
  });
});

test("auth log query omits filters unsupported by the auth endpoint", () => {
  assert.deepEqual(logsApi.buildSystemLogQuery("auth", {
    action: "LOGIN_FAILED", actorUserId: "user-2", username: "ignored",
    table: "ignored", rowId: "ignored", requestId: "ignored",
    fromAt: "", toAt: "",
  }, 1), {
    page: 0, size: 25, action: "LOGIN_FAILED", actorUserId: "user-2",
  });
});

test("business presentation uses actual occurrence time and request metadata", () => {
  assert.equal(typeof logsApi.presentSystemLog, "function");
  assert.deepEqual(logsApi.presentSystemLog({
    username: "admin", actorUserId: "user-1", actorType: "USER",
    action: "EMPLOYEE_UPDATE", occurredAt: "2026-10-06T01:02:03Z",
    createdAt: "2026-10-06T01:03:00", method: "PATCH", path: "/api/employees/1",
    rowCount: 2, changes: [{ table: "users" }, { table: "employees" }],
  }, "business"), {
    actor: "admin", timestamp: "2026-10-06T01:02:03Z",
    source: "PATCH /api/employees/1", summary: "2 dòng thay đổi", severity: "INFO",
  });
});

test("auth presentation maps failed login and anonymous user fields", () => {
  assert.deepEqual(logsApi.presentSystemLog({
    userId: null, action: "LOGIN_FAILED", description: "Sai mật khẩu",
    ipAddress: "10.0.0.8", createdAt: "2026-10-06T08:00:00",
  }, "auth"), {
    actor: "Ẩn danh", timestamp: "2026-10-06T08:00:00",
    source: "10.0.0.8", summary: "Sai mật khẩu", severity: "WARN",
  });
});
