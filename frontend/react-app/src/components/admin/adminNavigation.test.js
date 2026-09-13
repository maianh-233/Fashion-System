import test from "node:test";
import assert from "node:assert/strict";
import { buildAdminNavigation, findActiveNavigation } from "./adminNavigation.js";

test("catalog navigation exposes a dedicated Variant page when Variant view is granted", () => {
  const navigation = buildAdminNavigation([{
    code: "CATALOG",
    name: "Sản phẩm",
    groups: [{
      code: "PRODUCT",
      name: "Quản lý sản phẩm",
      permissions: [
        { code: "PRODUCT_VIEW" },
        { code: "PRODUCT_VARIANT_VIEW" },
      ],
    }],
  }]);

  assert.deepEqual(
    navigation[0].groups.map(({ code, path }) => ({ code, path })),
    [
      { code: "PRODUCT", path: "/admin/products" },
      { code: "PRODUCT_VARIANT", path: "/admin/product-variants" },
    ],
  );
  assert.equal(
    findActiveNavigation(navigation, "/admin/product-variants").activeGroup.code,
    "PRODUCT_VARIANT",
  );
});
