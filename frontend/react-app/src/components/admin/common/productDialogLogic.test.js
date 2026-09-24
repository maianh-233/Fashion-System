import assert from "node:assert/strict";
import test from "node:test";
import { createProductViewDialog } from "./productDialogLogic.js";

test("opens a selected collection product in the shared product dialog view mode", () => {
  const product = { id: 42, code: "SP-42", name: "Áo linen" };

  assert.deepEqual(createProductViewDialog(product), {
    mode: "view",
    product,
  });
});

test("keeps the product dialog closed when no product is selected", () => {
  assert.equal(createProductViewDialog(null), null);
});
