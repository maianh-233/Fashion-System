import assert from "node:assert/strict";
import test from "node:test";
import {
  catalogPageMetadata,
  isCatalogFieldVisible,
  isInactiveCatalogRow,
} from "./catalogPageLogic.js";

test("deleted supplier rows expose the restore action", () => {
  assert.equal(isInactiveCatalogRow({ status: "DELETED" }), true);
});

test("reads totals from Spring Data page DTO responses", () => {
  assert.deepEqual(catalogPageMetadata({
    content: [{ id: "supplier-1" }],
    page: { totalPages: 4, totalElements: 73, number: 0, size: 20 },
  }), { totalPages: 4, totalElements: 73 });
});

test("keeps compatibility with legacy top-level page totals", () => {
  assert.deepEqual(catalogPageMetadata({ totalPages: 2, totalElements: 25 }), {
    totalPages: 2,
    totalElements: 25,
  });
});

test("hides create-only image field without variant update permission", () => {
  const field = { key: "imageUrl", createOnly: true, permission: "PRODUCT_VARIANT_UPDATE" };
  assert.equal(isCatalogFieldVisible(field, "create", () => false), false);
  assert.equal(isCatalogFieldVisible(field, "create", (code) => code === "PRODUCT_VARIANT_UPDATE"), true);
});
