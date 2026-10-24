import test from "node:test";
import assert from "node:assert/strict";
import { orderActions, orderProgress, paymentCountdown } from "../../../src/pages/customer/customerOrderLogic.js";

test("progress marks reached steps and the current one", () => {
  const history = [
    { status: "PENDING", at: "2026-10-20T08:00:00" },
    { status: "CONFIRMED", at: "2026-10-20T09:00:00" },
  ];
  const { steps, offTrack } = orderProgress("CONFIRMED", history);
  assert.deepEqual(steps.map((s) => [s.status, s.done, s.current]), [
    ["PENDING", true, false], ["CONFIRMED", false, true], ["HANDED_TO_SHIPPER", false, false],
    ["SHIPPING", false, false], ["COMPLETED", false, false]]);
  assert.equal(steps[1].at, "2026-10-20T09:00:00");
  assert.equal(offTrack, null);
});

test("completed order has every step done; cancelled order is off track", () => {
  assert.ok(orderProgress("COMPLETED", []).steps.every((s) => s.done && !s.current));
  const cancelled = orderProgress("CANCELLED", [{ status: "PENDING", at: "a" }, { status: "CANCELLED", at: "b" }]);
  assert.deepEqual(cancelled.offTrack, { status: "CANCELLED", at: "b" });
  assert.equal(cancelled.steps[0].done, true);
});

test("actions follow the flags computed by the backend", () => {
  const actions = orderActions({ canCancel: true, canPay: false, canSwitchToCod: false,
    parts: [{ orderCode: "A", trackingUrl: "https://track/a", storeName: "S" }, { orderCode: "B", trackingUrl: null }] });
  assert.equal(actions.cancel, true);
  assert.deepEqual(actions.track, [{ code: "A", url: "https://track/a", store: "S" }]);
});

test("payment countdown", () => {
  const now = Date.parse("2026-10-20T08:00:00Z");
  assert.equal(paymentCountdown("2026-10-20T08:12:05Z", now), "12 phút 05 giây");
  assert.equal(paymentCountdown("2026-10-20T07:59:00Z", now), "đã hết hạn");
  assert.equal(paymentCountdown(null, now), null);
});
