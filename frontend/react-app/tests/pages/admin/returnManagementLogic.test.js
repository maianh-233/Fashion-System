import test from "node:test";
import assert from "node:assert/strict";
import { availableActions, defaultConditions, formatMoney } from "../../../src/pages/admin/returnManagementLogic.js";

test("actions follow the request status", () => {
  assert.deepEqual(availableActions({ status: "REQUESTED" }), ["approve", "reject"]);
  assert.deepEqual(availableActions({ status: "APPROVED" }), ["complete", "reject"]);
  assert.deepEqual(availableActions({ status: "COMPLETED", refundStatus: "PENDING" }), ["confirmRefund"]);
  assert.deepEqual(availableActions({ status: "COMPLETED", refundStatus: "COMPLETED" }), []);
  assert.deepEqual(availableActions(null), []);
});

test("items default to good condition and money formats in VND", () => {
  assert.deepEqual(defaultConditions([{ id: "x" }, { id: "y", condition: "DAMAGED" }]), { x: "GOOD", y: "DAMAGED" });
  assert.equal(formatMoney(270000), `${(270000).toLocaleString("vi-VN")} ₫`);
  assert.equal(formatMoney(null), "");
});

test("size report range covers the last N days including today", async () => {
  const { reportRange } = await import("../../../src/pages/admin/returnManagementLogic.js");
  assert.deepEqual(reportRange(30, new Date(2026, 10, 2)), { from: "2026-10-04", to: "2026-11-02" });
  assert.deepEqual(reportRange(1, new Date(2026, 0, 1)), { from: "2026-01-01", to: "2026-01-01" });
});

test("size report CSV has one row per product size and escapes commas", async () => {
  const { sizeReportCsv, rateTone } = await import("../../../src/pages/admin/returnManagementLogic.js");
  const csv = sizeReportCsv({ products: [{ productName: "Áo, sơ mi", categoryName: "Áo", sizes: [
    { size: "M", soldQuantity: 20, returnedQuantity: 4, returnRate: 20, wrongSizeQuantity: 4, wrongSizeRate: 20,
      exchanges: [{ toSize: "L", quantity: 3 }] },
  ] }] });
  const lines = csv.replace("\uFEFF", "").split("\n");
  assert.equal(lines.length, 2);
  assert.equal(lines[1], "\"Áo, sơ mi\",Áo,M,20,4,20,4,20,L ×3");
  assert.equal(rateTone(12), "text-rose-300");
  assert.equal(rateTone(6), "text-amber-300");
  assert.equal(rateTone(1), "text-zinc-300");
});
