import assert from "node:assert/strict";
import test from "node:test";
import { supplierApi } from "./catalogApi.js";

test("supplier delete does not require an unsupported impact endpoint", () => {
  assert.equal(supplierApi.impact, null);
});
