export const productTableColumns = [
  { key: "imageUrl", label: "Ảnh" },
  { key: "code", label: "Mã sản phẩm" },
  { key: "name", label: "Sản phẩm", minWidth: 220 },
  { key: "slug", label: "Slug" },
  { key: "material", label: "Chất liệu" },
  { key: "fit", label: "Form" },
  { key: "gender", label: "Giới tính" },
  { key: "status", label: "Trạng thái" },
];

export function productVariantAction(product) {
  return {
    to: `/admin/product-variants?productId=${product.id}`,
    title: "Xem biến thể",
    ariaLabel: `Xem biến thể của ${product.name || product.code || product.id}`,
  };
}
