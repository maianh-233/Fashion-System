import assert from "node:assert/strict";
import test from "node:test";
import { buildAdminNavigation, findActiveNavigation } from "./adminNavigation.js";

test("sidebar excludes variants while keeping the product and promotion groups", () => {
  const navigation = buildAdminNavigation([{
    code: "CATALOG",
    name: "Sản phẩm",
    groups: [
      { code: "PRODUCT", name: "Quản lý sản phẩm", permissions: [{ code: "PRODUCT_VARIANT_VIEW" }] },
      { code: "PRODUCT_VARIANT", name: "Quản lý biến thể", permissions: [] },
    ],
  }, {
    code: "SALES",
    name: "Bán hàng",
    groups: [{ code: "PROMOTION", name: "Khuyến mãi", permissions: [] }],
  }]);

  assert.deepEqual(navigation[0].groups.map((group) => group.code), ["PRODUCT"]);
  assert.deepEqual(navigation[1].groups.map((group) => group.code), ["PROMOTION"]);
  assert.equal(findActiveNavigation(navigation, "/admin/product-variants").activeGroup.code, "PRODUCT");
});

test("demo sidebar excludes variants and keeps promotions", () => {
  const navigation = buildAdminNavigation([], true);
  assert.equal(navigation.some((module) => module.groups.some((group) => group.code === "PRODUCT_VARIANT")), false);
  assert.equal(navigation.some((module) => module.groups.some((group) => group.code === "PROMOTION")), true);
});
