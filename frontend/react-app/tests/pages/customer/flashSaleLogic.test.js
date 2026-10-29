import test from "node:test";
import assert from "node:assert/strict";
import { countdown, flashByVariant, percentOff, soldPercent } from "../../../src/pages/customer/flashSaleLogic.js";

test("countdown formats hh:mm:ss and stops at zero", () => {
  const now = Date.parse("2026-10-29T10:00:00Z");
  assert.deepEqual(countdown("2026-10-29T11:02:03Z", now), { done: false, hours: 1, minutes: 2, seconds: 3, text: "01:02:03" });
  assert.equal(countdown("2026-10-29T09:00:00Z", now).done, true);
  assert.equal(countdown("2026-10-29T09:00:00Z", now).text, "00:00:00");
});

test("variant map prefers the sale ending first", () => {
  const sales = [
    { name: "Late", endsAt: "2026-10-29T12:00:00Z", items: [{ variantId: "v1", salePrice: 100 }] },
    { name: "Soon", endsAt: "2026-10-29T11:00:00Z", items: [{ variantId: "v1", salePrice: 90 }, { variantId: "v2", salePrice: 50 }] },
  ];
  const map = flashByVariant(sales);
  assert.equal(map.v1.saleName, "Soon");
  assert.equal(map.v1.salePrice, 90);
  assert.equal(map.v2.endsAt, "2026-10-29T11:00:00Z");
});

test("discount percent and sold progress", () => {
  assert.equal(percentOff(400000, 250000), 38);
  assert.equal(percentOff(0, 10), 0);
  assert.equal(soldPercent({ quota: 4, remaining: 1 }), 75);
  assert.equal(soldPercent({ quota: 0, remaining: 0 }), 0);
});
