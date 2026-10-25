import test from "node:test";
import assert from "node:assert/strict";
import { tierForm, validateTier, tierPayload, spendRange } from "../../../src/pages/admin/customerTierLogic.js";

const tiers = [
  { id: "r", code: "REGULAR", minTotalSpent: 0 },
  { id: "s", code: "SILVER", minTotalSpent: 5000000 },
  { id: "g", code: "GOLD", minTotalSpent: 20000000 },
];
const form = (overrides = {}) => ({ code: "VIP", name: "VIP", minTotalSpent: "50000000", discountPercent: "5", ...overrides });

test("valid tier passes and payload normalizes code and numbers", () => {
  assert.equal(validateTier(form({ code: " vip_1 " }), tiers), "");
  assert.deepEqual(tierPayload(form({ code: " vip_1 ", name: " Kim cương " })),
    { code: "VIP_1", name: "Kim cương", minTotalSpent: 50000000, discountPercent: 5 });
});

test("rejects bad codes, missing names and out-of-range numbers", () => {
  assert.match(validateTier(form({ code: "" }), tiers), /mã hạng/);
  assert.match(validateTier(form({ code: "Vàng" }), tiers), /không dấu/);
  assert.match(validateTier(form({ name: " " }), tiers), /tên hạng/);
  assert.match(validateTier(form({ minTotalSpent: "-1" }), tiers), /không âm/);
  assert.match(validateTier(form({ minTotalSpent: "" }), tiers), /không âm/);
  assert.match(validateTier(form({ discountPercent: "101" }), tiers), /0–100/);
});

test("rejects duplicate code or threshold but allows the tier being edited", () => {
  assert.equal(validateTier(form({ code: "gold" }), tiers), "Mã hạng đã tồn tại.");
  assert.match(validateTier(form({ minTotalSpent: "5000000" }), tiers), /ngưỡng/);
  assert.equal(validateTier(form({ code: "GOLD", minTotalSpent: "20000000" }), tiers, "g"), "");
});

test("default tier keeps its code and zero threshold", () => {
  assert.match(validateTier(form({ code: "BASIC", minTotalSpent: "0" }), tiers, "r"), /REGULAR/);
  assert.match(validateTier(form({ code: "REGULAR", minTotalSpent: "100" }), tiers, "r"), /REGULAR/);
  assert.equal(validateTier(form({ code: "REGULAR", name: "Thành viên", minTotalSpent: "0" }), tiers, "r"), "");
});

test("form defaults and spend ranges", () => {
  assert.deepEqual(tierForm(null), { code: "", name: "", minTotalSpent: "", discountPercent: "0" });
  assert.equal(tierForm({ code: "GOLD", name: "Vàng", minTotalSpent: "20000000.00", discountPercent: null }).minTotalSpent, "20000000");
  assert.equal(spendRange(tiers, 1), "Từ 5.000.000đ đến dưới 20.000.000đ");
  assert.equal(spendRange(tiers, 2), "Từ 20.000.000đ trở lên");
});
