import { brandApi, categoryApi, collectionApi } from "../../../api/catalogApi";

export const productDialogFields = [
  { key: "name", label: "Tên sản phẩm", required: true, fullWidth: true },
  { key: "code", label: "Mã sản phẩm", generated: true },
  { key: "slug", label: "Slug", generated: true },
  { key: "brandId", label: "Thương hiệu", type: "catalog", api: brandApi },
  { key: "categoryId", label: "Danh mục", type: "catalog", api: categoryApi },
  { key: "collectionId", label: "Bộ sưu tập", type: "catalog", api: collectionApi, fullWidth: true },
  { key: "material", label: "Chất liệu" },
  { key: "fit", label: "Form" },
  { key: "gender", label: "Giới tính", type: "select", options: [{ value: "MALE", label: "Nam" }, { value: "FEMALE", label: "Nữ" }, { value: "UNISEX", label: "Unisex" }] },
  { key: "status", label: "Trạng thái", type: "select", options: [{ value: "ACTIVE", label: "Đang bán" }, { value: "DRAFT", label: "Bản nháp" }, { value: "ARCHIVE", label: "Lưu trữ" }] },
  { key: "imageUrl", label: "Ảnh sản phẩm", type: "image" },
  { key: "description", label: "Mô tả", type: "textarea" },
];
