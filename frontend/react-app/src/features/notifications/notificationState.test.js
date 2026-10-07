import assert from "node:assert/strict";
import test from "node:test";
import { mergeNotifications, notificationFromRealtime } from "./notificationState.js";

const older = {
  id: "notification-1",
  eventId: "event-1",
  type: "SYSTEM",
  title: "Older",
  message: "Older message",
  createdAt: "2026-10-08T09:00:00Z",
};
const newer = {
  id: "notification-2",
  eventId: "event-2",
  type: "SYSTEM",
  title: "Newer",
  message: "Newer message",
  createdAt: "2026-10-08T10:00:00Z",
};

test("merges pages newest-first while deduplicating by notification id", () => {
  const merged = mergeNotifications([newer], [older, newer]);

  assert.deepEqual(merged.map((item) => item.id), ["notification-2", "notification-1"]);
});

test("realtime followed by first-page refetch remains one stable item", () => {
  const afterRealtime = mergeNotifications([older], [newer]);
  const afterRefetch = mergeNotifications(afterRealtime, [newer, older]);

  assert.deepEqual(afterRefetch.map((item) => item.id), ["notification-2", "notification-1"]);
});

test("extracts only valid notification payloads from generic realtime envelopes", () => {
  assert.equal(notificationFromRealtime({ channel: "ORDER", eventType: "NOTIFICATION_CREATED", payload: newer }), null);
  assert.equal(notificationFromRealtime({ channel: "NOTIFICATION", eventType: "NOTIFICATION_CREATED", payload: {} }), null);
  assert.deepEqual(notificationFromRealtime({
    channel: "NOTIFICATION",
    eventType: "NOTIFICATION_CREATED",
    payload: newer,
  }), newer);
});
