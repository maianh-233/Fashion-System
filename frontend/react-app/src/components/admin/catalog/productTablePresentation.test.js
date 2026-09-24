import assert from "node:assert/strict";
import test from "node:test";
import {
  productTableColumns,
  productVariantAction,
} from "./productTablePresentation.js";

test("uses one product column definition for the main page and collection tab", () => {
  assert.deepEqual(
    productTableColumns.map(({ key, label, minWidth }) => ({ key, label, minWidth })),
    [
      { key: "imageUrl", label: "Ảnh", minWidth: undefined },
      { key: "code", label: "Mã sản phẩm", minWidth: undefined },
      { key: "name", label: "Sản phẩm", minWidth: 220 },
      { key: "slug", label: "Slug", minWidth: undefined },
      { key: "material", label: "Chất liệu", minWidth: undefined },
      { key: "fit", label: "Form", minWidth: undefined },
      { key: "gender", label: "Giới tính", minWidth: undefined },
      { key: "status", label: "Trạng thái", minWidth: undefined },
    ],
  );
});

test("describes the variant navigation as an icon-only accessible action", () => {
  assert.deepEqual(productVariantAction({ id: 42, name: "Áo linen" }), {
    to: "/admin/product-variants?productId=42",
    title: "Xem biến thể",
    ariaLabel: "Xem biến thể của Áo linen",
  });
});
