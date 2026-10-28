import test from "node:test";
import assert from "node:assert/strict";
import { buildReturnPayload, estimateRefund, initialSelection, validateReturnForm } from "../../../src/pages/customer/returnLogic.js";

const items = [
  { orderItemId: "a", productName: "Quần", requestable: 2, unitPrice: "270000" },
  { orderItemId: "b", productName: "Áo", requestable: 1, unitPrice: "150000" },
];

test("refund estimate counts only picked lines", () => {
  const selection = { ...initialSelection(items), a: { checked: true, quantity: 2, exchangeVariantId: "" } };
  assert.equal(estimateRefund(items, selection), 540000);
  assert.equal(estimateRefund(items, initialSelection(items)), 0);
});

test("validation: pick something, a reason, a valid quantity and an exchange target", () => {
  const none = initialSelection(items);
  assert.match(validateReturnForm({ type: "RETURN", reasonCode: "OTHER", items, selection: none }), /ít nhất một/);
  const picked = { ...none, b: { checked: true, quantity: 1, exchangeVariantId: "" } };
  assert.match(validateReturnForm({ type: "RETURN", reasonCode: "", items, selection: picked }), /lý do/);
  assert.equal(validateReturnForm({ type: "RETURN", reasonCode: "OTHER", items, selection: picked }), "");
  assert.match(validateReturnForm({ type: "EXCHANGE", reasonCode: "WRONG_SIZE", items, selection: picked }), /phân loại/);
  const tooMany = { ...none, b: { checked: true, quantity: 2, exchangeVariantId: "" } };
  assert.match(validateReturnForm({ type: "RETURN", reasonCode: "OTHER", items, selection: tooMany }), /từ 1 đến 1/);
});

test("payload keeps exchange targets only for exchanges", () => {
  const selection = { a: { checked: true, quantity: 1, exchangeVariantId: "v2" }, b: { checked: false, quantity: 1, exchangeVariantId: "" } };
  assert.deepEqual(buildReturnPayload({ orderId: "o", type: "EXCHANGE", reasonCode: "WRONG_SIZE", note: "  ", items, selection }),
    { orderId: "o", type: "EXCHANGE", reasonCode: "WRONG_SIZE", note: null, items: [{ orderItemId: "a", quantity: 1, exchangeVariantId: "v2" }] });
  assert.equal(buildReturnPayload({ orderId: "o", type: "RETURN", reasonCode: "OTHER", items, selection }).items[0].exchangeVariantId, null);
});
