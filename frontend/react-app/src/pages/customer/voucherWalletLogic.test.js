import test from "node:test";
import assert from "node:assert/strict";
import { canRedeem, formatPoints, missingPoints, toCardPromotion, voucherState } from "./voucherWalletLogic.js";

test("tier targets become names for the storefront card", () => {
  assert.deepEqual(toCardPromotion({ code: "A", tiers: [{ id: "1", name: "Vàng" }, "Bạc", null] }).tiers, ["Vàng", "Bạc"]);
  assert.deepEqual(toCardPromotion({ code: "A" }).tiers, []);
  assert.equal(toCardPromotion(null), null);
});

test("redeem needs enough points", () => {
  assert.equal(canRedeem({ pointsRequired: 100 }, 100), true);
  assert.equal(canRedeem({ pointsRequired: 100 }, 99), false);
  assert.equal(canRedeem({ pointsRequired: null }, 500), false);
  assert.equal(missingPoints({ pointsRequired: 100 }, 40), 60);
  assert.equal(missingPoints({ pointsRequired: 100 }, 400), 0);
});

test("owned voucher state", () => {
  const now = new Date("2026-10-22T00:00:00");
  const active = { status: "ACTIVE", endDate: "2026-11-01T00:00:00" };
  assert.equal(voucherState({ promotion: active }, now).key, "active");
  assert.equal(voucherState({ promotion: active, usedOrderId: "o1" }, now).key, "used");
  assert.equal(voucherState({ promotion: { ...active, endDate: "2026-10-01T00:00:00" } }, now).key, "expired");
  assert.equal(voucherState({ promotion: { ...active, status: "INACTIVE" } }, now).key, "expired");
  assert.equal(voucherState({ promotion: null }, now).key, "expired");
  assert.equal(formatPoints(1500), `${(1500).toLocaleString("vi-VN")} điểm`);
});
