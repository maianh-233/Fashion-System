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
