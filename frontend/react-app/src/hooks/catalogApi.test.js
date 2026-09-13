import test from "node:test";
import assert from "node:assert/strict";
import { buildCatalogQuery } from "./catalogQuery.js";
import { toCatalogOptions } from "./catalogQuery.js";

test("buildCatalogQuery omits empty filters and preserves database page parameters", () => {
  assert.equal(
    buildCatalogQuery({ keyword: "áo sơ mi", status: "", page: 2, size: 20 }),
    "?keyword=%C3%A1o+s%C6%A1+mi&page=2&size=20",
  );
});

test("buildCatalogQuery serializes false without treating it as empty", () => {
  assert.equal(buildCatalogQuery({ active: false }), "?active=false");
});

test("toCatalogOptions creates readable Product choices and keeps ids as values", () => {
  assert.deepEqual(toCatalogOptions([
    { id: "product-1", name: "Áo sơ mi Linen", slug: "ao-so-mi-linen" },
    { id: "product-2", name: "Quần Jeans", slug: null },
  ]), [
    { value: "product-1", label: "Áo sơ mi Linen · ao-so-mi-linen" },
    { value: "product-2", label: "Quần Jeans" },
  ]);
});
