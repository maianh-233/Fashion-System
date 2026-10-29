import test from "node:test";
import assert from "node:assert/strict";
import { groupRequests, suggestedQuantity, transferActions } from "../../../src/pages/admin/transferLogic.js";

test("suggested quantity fills up to the target without draining the source", () => {
  assert.equal(suggestedQuantity({ total: 2, incoming: 0, suggestedStock: 20 }), 8);
  assert.equal(suggestedQuantity({ total: 2, incoming: 6, suggestedStock: 20 }), 2);
  assert.equal(suggestedQuantity({ total: 0, incoming: 0, suggestedStock: 8 }), 3);
  assert.equal(suggestedQuantity({ total: 9, incoming: 4, suggestedStock: 50 }), 0);
  assert.equal(suggestedQuantity({ total: 1, suggestedStock: null }), 0);
});

test("picked rows are grouped per suggested source store", () => {
  const a = { id: "a", name: "A" };
  const rows = [
    { variantId: "v1", suggestedStore: a }, { variantId: "v2", suggestedStore: a },
    { variantId: "v3", suggestedStore: { id: "b", name: "B" } }, { variantId: "v4", suggestedStore: null },
  ];
  const groups = groupRequests(rows, { v1: 3, v2: 0, v3: 2, v4: 5 });
  assert.deepEqual(groups, [
    { fromStoreId: "a", fromStoreName: "A", items: [{ variantId: "v1", quantity: 3 }] },
    { fromStoreId: "b", fromStoreName: "B", items: [{ variantId: "v3", quantity: 2 }] },
  ]);
});

test("actions depend on status and on which side the user stands", () => {
  const transfer = { status: "REQUESTED", fromStore: { id: "from" }, toStore: { id: "to" } };
  assert.deepEqual(transferActions(transfer, { isGlobal: false, currentStoreId: "from" }), ["ship", "reject"]);
  assert.deepEqual(transferActions(transfer, { isGlobal: false, currentStoreId: "to" }), ["cancel"]);
  assert.deepEqual(transferActions(transfer, { isGlobal: true }), ["ship", "reject", "cancel"]);
  assert.deepEqual(transferActions({ ...transfer, status: "SHIPPED" }, { isGlobal: false, currentStoreId: "from" }), []);
  assert.deepEqual(transferActions({ ...transfer, status: "SHIPPED" }, { isGlobal: false, currentStoreId: "to" }), ["receive"]);
  assert.deepEqual(transferActions({ ...transfer, status: "RECEIVED" }, { isGlobal: true }), []);
});
