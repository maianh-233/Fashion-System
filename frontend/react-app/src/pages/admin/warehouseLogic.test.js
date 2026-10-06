import test from "node:test";
import assert from "node:assert/strict";
import * as warehouseLogic from "./warehouseLogic.js";
const { receiptActions, validateHeader, validateItem, headerPayload, fixedSource, isReceiptEditable } = warehouseLogic;
test("receipt editing remains available before confirmation only", () => {
  assert.equal(isReceiptEditable("DRAFT"), true);
  assert.equal(isReceiptEditable("PENDING_CONFIRMATION"), true);
  for (const status of ["CONFIRMED", "COMPLETED", "CANCELLED"]) assert.equal(isReceiptEditable(status), false);
});
test("receipt transitions require the correct status and individual permission", () => {
  assert.deepEqual(receiptActions("CONFIRMED", "import", p => p === "IMPORT_RECEIPT_COMPLETE").map(a => a[0]), ["complete"]);
  assert.deepEqual(receiptActions("CONFIRMED", "export", () => true).map(a => a[0]), ["complete"]);
  assert.deepEqual(receiptActions("COMPLETED", "export", () => true), []);
  assert.deepEqual(receiptActions("CANCELLED", "import", () => true), []);
  assert.deepEqual(receiptActions("DRAFT", "export", p => p === "EXPORT_RECEIPT_CONFIRM"), []);
});
test("required headers and transfer directions", () => {
  assert.ok(validateHeader({ storeId: "s" }, "import"));
  assert.ok(validateHeader({ storeId: "s", issueType: "RETURN_TO_SUPPLIER" }, "export"));
  assert.ok(validateHeader({ storeId: "s", issueType: "OTHER", reason: " " }, "export"));
  assert.equal(validateHeader({ storeId: "s", issueType: "DAMAGED" }, "export"), "");
  assert.equal(fixedSource("ONLINE_TO_OFFLINE"), "ONLINE");
  assert.equal(fixedSource("OFFLINE_TO_ONLINE"), "OFFLINE");
  assert.equal(headerPayload({ storeId: "s", status: "COMPLETED", receivedBy: "forged" }, "import").status, undefined);
});
test("export availability aggregates duplicate variant and source, excludes edited row", () => {
  const row = { id: "a", productId: "p", productVariantId: "v", quantity: 4, sourceChannel: "ONLINE" };
  const items = [row, { ...row, id: "b", quantity: 3 }, { ...row, id: "c", quantity: 100, sourceChannel: "OFFLINE" }];
  assert.equal(validateItem(row, "export", items, { onlineQuantity: 7 }), "");
  assert.match(validateItem({ ...row, quantity: 5 }, "export", items, { onlineQuantity: 7 }), /Vượt tồn/);
  assert.ok(validateItem(row, "export", [], null));
  assert.ok(validateItem(row, "export", [], { onlineQuantity: 10 }, "OFFLINE_TO_ONLINE"));
});
test("line quantities and costs reject invalid numbers", () => {
  const row = { productId: "p", productVariantId: "v", quantity: 1, targetChannel: "OFFLINE", costPrice: 0 };
  assert.equal(validateItem(row, "import"), "");
  for (const quantity of [0, -1, 1.5, 2147483648, "invalid"]) assert.ok(validateItem({ ...row, quantity }, "import"));
  assert.ok(validateItem({ ...row, costPrice: -1 }, "import"));
  assert.ok(validateItem({ ...row, costPrice: "12.345" }, "import"));
  assert.ok(validateItem({ ...row, costPrice: "10000000000" }, "import"));
  assert.ok(validateItem({ ...row, costPrice: "9999999999.99", quantity: 101 }, "import"));
  assert.ok(validateItem(row, "import", [{ ...row, id: "old", quantity: 2147483647 }]));
});
test("receipt date filters use date-only API values", () => {
  assert.equal(typeof warehouseLogic.receiptDateParams, "function");
  assert.deepEqual(warehouseLogic.receiptDateParams("2026-10-01", "2026-10-06"), {
    fromDate: "2026-10-01",
    toDate: "2026-10-06",
  });
  assert.deepEqual(warehouseLogic.receiptDateParams("", ""), {});
});
