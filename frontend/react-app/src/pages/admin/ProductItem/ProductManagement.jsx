import { Shirt } from "lucide-react";
import ApiCatalogPage from "../../../components/admin/catalog/ApiCatalogPage";
import { productApi } from "../../../hooks/catalogApi";
import { useNavigate } from "react-router-dom";
import ProductMetadataPanel from "../../../components/admin/catalog/ProductMetadataPanel";

const columns = [{ key: "name", label: "Sản phẩm" }, { key: "slug", label: "Slug" }, { key: "material", label: "Chất liệu" }, { key: "fit", label: "Form" }, { key: "gender", label: "Giới tính" }, { key: "status", label: "Trạng thái" }];
const fields = [
  { key: "name", label: "Tên sản phẩm", required: true }, { key: "slug", label: "Slug" },
  { key: "brandId", label: "UUID thương hiệu" }, { key: "collectionId", label: "UUID bộ sưu tập" },
  { key: "categoryId", label: "UUID danh mục" }, { key: "material", label: "Chất liệu" },
  { key: "fit", label: "Form" }, { key: "gender", label: "Giới tính", type: "select", options: [{ value: "MALE", label: "Nam" }, { value: "FEMALE", label: "Nữ" }, { value: "UNISEX", label: "Unisex" }] },
  { key: "status", label: "Trạng thái", type: "select", options: [{ value: "ACTIVE", label: "Đang bán" }, { value: "DRAFT", label: "Bản nháp" }, { value: "ARCHIVE", label: "Lưu trữ" }] },
  { key: "description", label: "Mô tả", type: "textarea" },
];
const permissions = { create: "PRODUCT_CREATE", update: "PRODUCT_UPDATE", delete: "PRODUCT_DELETE" };
const normalizeProduct = (value) => ({ ...value, brandId: value.brandId || null, collectionId: value.collectionId || null, categoryId: value.categoryId || null, imageUrl: value.imageUrl || null });

export default function ProductManagement() {
  const navigate = useNavigate();
  return <ApiCatalogPage title="Sản phẩm" description="Catalog Product thật từ backend; Store user chỉ được đọc." icon={Shirt} api={productApi} permissions={permissions} columns={columns} fields={fields} initialValues={{ status: "DRAFT", gender: "UNISEX" }} normalize={normalizeProduct} onOpenRelated={(product) => navigate(`/admin/product-variants?productId=${product.id}`)} relatedLabel="Xem biến thể" relatedPermission="PRODUCT_VARIANT_VIEW" renderDetails={(product) => <ProductMetadataPanel productId={product.id} />} />;
}
