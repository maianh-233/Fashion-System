import { Shirt } from "lucide-react";
import ApiCatalogPage from "../../../components/admin/catalog/ApiCatalogPage";
import { brandApi, categoryApi, collectionApi, productApi, tagApi } from "../../../hooks/catalogApi";
import { useNavigate } from "react-router-dom";
import ProductDetailTabs from "../../../components/admin/catalog/ProductDetailTabs";

const columns = [{ key: "imageUrl", label: "Ảnh", render: (row) => row.imageUrl ? <img src={row.imageUrl} alt={row.name} className="h-10 w-10 rounded-lg object-cover" /> : "—" }, { key: "code", label: "Mã sản phẩm" }, { key: "name", label: "Sản phẩm" }, { key: "slug", label: "Slug" }, { key: "material", label: "Chất liệu" }, { key: "fit", label: "Form" }, { key: "gender", label: "Giới tính" }, { key: "status", label: "Trạng thái" }];
const fields = [
  { key: "code", label: "Mã sản phẩm", generated: true }, { key: "name", label: "Tên sản phẩm", required: true }, { key: "slug", label: "Slug", generated: true },
  { key: "brandId", label: "Thương hiệu", type: "catalog", api: brandApi }, { key: "collectionId", label: "Bộ sưu tập", type: "catalog", api: collectionApi },
  { key: "categoryId", label: "Danh mục", type: "catalog", api: categoryApi }, { key: "material", label: "Chất liệu" },
  { key: "fit", label: "Form" }, { key: "gender", label: "Giới tính", type: "select", options: [{ value: "MALE", label: "Nam" }, { value: "FEMALE", label: "Nữ" }, { value: "UNISEX", label: "Unisex" }] },
  { key: "status", label: "Trạng thái", type: "select", options: [{ value: "ACTIVE", label: "Đang bán" }, { value: "DRAFT", label: "Bản nháp" }, { value: "ARCHIVE", label: "Lưu trữ" }] },
  { key: "imageUrl", label: "Ảnh sản phẩm", type: "image" },
  { key: "description", label: "Mô tả", type: "textarea" },
];
const permissions = { create: "PRODUCT_CREATE", update: "PRODUCT_UPDATE", delete: "PRODUCT_DELETE" };
const filterFields = [
  { key: "brandId", label: "Thương hiệu", api: brandApi },
  { key: "categoryId", label: "Danh mục", api: categoryApi },
  { key: "collectionId", label: "Bộ sưu tập", api: collectionApi },
  { key: "tagId", label: "Tag", api: tagApi },
];
const normalizeProduct = (value) => ({ ...value, brandId: value.brandId || null, collectionId: value.collectionId || null, categoryId: value.categoryId || null, imageUrl: value.imageUrl || null });

export default function ProductManagement() {
  const navigate = useNavigate();
  return <ApiCatalogPage filterFields={filterFields} statusParam="status" statusOptions={[{ value: "DRAFT", label: "Bản nháp" }, { value: "ARCHIVE", label: "Lưu trữ" }, { value: "ALL", label: "Tất cả" }]} title="Sản phẩm" description="Catalog Product thật từ backend; Store user chỉ được đọc." icon={Shirt} api={productApi} permissions={permissions} columns={columns} fields={fields} initialValues={{ status: "ACTIVE", gender: "UNISEX" }} normalize={normalizeProduct} onOpenRelated={(product) => navigate(`/admin/product-variants?productId=${product.id}`)} relatedLabel="Xem biến thể" relatedPermission="PRODUCT_VARIANT_VIEW" renderDetails={(product) => <ProductDetailTabs product={product} />} />;
}
