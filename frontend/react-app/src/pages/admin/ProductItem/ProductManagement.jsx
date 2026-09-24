import { Shirt } from "lucide-react";
import ApiCatalogPage from "../../../components/admin/catalog/ApiCatalogPage";
import { brandApi, categoryApi, collectionApi, productApi, tagApi } from "../../../api/catalogApi";
import { useNavigate } from "react-router-dom";
import ProductDetailTabs from "../../../components/admin/catalog/ProductDetailTabs";
import ProductDialog from "../../../components/admin/common/ProductDialog";
import { productDialogFields } from "../../../components/admin/common/productDialogConfig";
import {
  productTableColumns,
  productVariantAction,
} from "../../../components/admin/catalog/productTablePresentation";

const columns = productTableColumns.map((column) => column.key === "imageUrl"
  ? { ...column, render: (row) => row.imageUrl ? <img src={row.imageUrl} alt={row.name} className="h-10 w-10 rounded-lg object-cover" /> : "—" }
  : column);
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
  const openVariants = (product) => navigate(productVariantAction(product).to);
  return <ApiCatalogPage filterFields={filterFields} statusParam="status" statusOptions={[{ value: "DRAFT", label: "Bản nháp" }, { value: "ARCHIVE", label: "Lưu trữ" }, { value: "ALL", label: "Tất cả" }]} title="Sản phẩm" description="Catalog Product thật từ backend; Store user chỉ được đọc." icon={Shirt} api={productApi} permissions={permissions} columns={columns} tableMinWidth={1400} dialogMaxWidth="max-w-5xl" continueEditingAfterCreate fields={productDialogFields} initialValues={{ status: "ACTIVE", gender: "UNISEX" }} normalize={normalizeProduct} onOpenRelated={openVariants} relatedLabel="Xem biến thể" relatedPermission="PRODUCT_VARIANT_VIEW" relatedIconOnly dialogComponent={ProductDialog} renderDetails={(product, mode) => <ProductDetailTabs key={product.id} product={product} mode={mode} />} />;
}
