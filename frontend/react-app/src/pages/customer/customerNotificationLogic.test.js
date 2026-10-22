import test from "node:test";
import assert from "node:assert/strict";
import { badgeLabel, markReadLocally, notificationTarget, prependNotification, relativeTime } from "./customerNotificationLogic.js";

test("realtime notifications go on top without duplicates", () => {
  const list = [{ id: "a" }, { id: "b" }];
  assert.deepEqual(prependNotification(list, { id: "b", title: "new" }).map((n) => n.id), ["b", "a"]);
  assert.equal(prependNotification(Array.from({ length: 10 }, (_, i) => ({ id: String(i) })), { id: "x" }).length, 10);
  assert.equal(prependNotification(list, null), list);
});

test("mark read one or all", () => {
  const list = [{ id: "a", read: false }, { id: "b", read: false }];
  assert.deepEqual(markReadLocally(list, "a").map((n) => n.read), [true, false]);
  assert.deepEqual(markReadLocally(list).map((n) => n.read), [true, true]);
});

test("targets stay inside the storefront", () => {
  assert.equal(notificationTarget({ actionUrl: "/orderdetail?id=1" }), "/orderdetail?id=1");
  assert.equal(notificationTarget({ actionUrl: "https://evil.example" }), "/orders");
  assert.equal(notificationTarget({ actionUrl: "//evil.example" }), "/orders");
  assert.equal(notificationTarget({ actionUrl: "/admin/orders", entityType: "ORDER", entityId: "9" }), "/orderdetail?id=9");
});

test("relative time and badge", () => {
  const now = Date.parse("2026-10-22T08:00:00Z");
  assert.equal(relativeTime("2026-10-22T07:59:40Z", now), "vừa xong");
  assert.equal(relativeTime("2026-10-22T07:55:00Z", now), "5 phút trước");
  assert.equal(relativeTime("2026-10-22T05:00:00Z", now), "3 giờ trước");
  assert.equal(badgeLabel(120), "99+");
});
