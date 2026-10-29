import test from "node:test";
import assert from "node:assert/strict";
import { emptyForm, salePayload, toInputTime, validateSaleForm } from "../../../src/pages/admin/flashSaleManagementLogic.js";

test("empty form starts at the next full hour and lasts two hours", () => {
  const form = emptyForm(new Date(2026, 9, 29, 10, 25));
  assert.equal(form.startsAt, "2026-10-29T11:00");
  assert.equal(form.endsAt, "2026-10-29T13:00");
  assert.equal(toInputTime(new Date(2026, 0, 2, 3, 4)), "2026-01-02T03:04");
});

test("validation and payload", () => {
  const item = { variantId: "v", label: "Váy", regularPrice: 400000, salePrice: "250000", quota: "5", sold: 0 };
  const form = { name: " Giờ vàng ", startsAt: "2026-10-29T11:00", endsAt: "2026-10-29T13:00", items: [item] };
  assert.equal(validateSaleForm(form), "");
  assert.match(validateSaleForm({ ...form, endsAt: "2026-10-29T10:00" }), /kết thúc/);
  assert.match(validateSaleForm({ ...form, items: [{ ...item, salePrice: "400000" }] }), /thấp hơn/);
  assert.match(validateSaleForm({ ...form, items: [{ ...item, quota: "2", sold: 3 }] }), /từ 3/);
  assert.deepEqual(salePayload(form), { name: "Giờ vàng", startsAt: "2026-10-29T11:00:00", endsAt: "2026-10-29T13:00:00",
    items: [{ variantId: "v", salePrice: 250000, quota: 5 }] });
});
