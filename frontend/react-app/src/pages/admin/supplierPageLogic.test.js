import assert from "node:assert/strict";
import test from "node:test";
import { isSupplierRestorable } from "./supplierPageLogic.js";

test("only soft-deleted suppliers use the restore action", () => {
  assert.equal(isSupplierRestorable({ status: "ACTIVE" }), false);
  assert.equal(isSupplierRestorable({ status: "INACTIVE" }), false);
  assert.equal(isSupplierRestorable({ status: "DELETED" }), true);
});
