import { GalleryVerticalEnd } from "lucide-react";
import ApiCatalogPage from "../../../components/admin/catalog/ApiCatalogPage";
import { brandApi, collectionApi } from "../../../hooks/catalogApi";
import CollectionProductsPanel from "../../../components/admin/catalog/CollectionProductsPanel";

const columns = [{ key: "imageUrl", label: "Ảnh", render: (row) => row.imageUrl ? <img src={row.imageUrl} alt={row.name} className="h-10 w-10 rounded-lg object-cover" /> : "—" }, { key: "code", label: "Mã" }, { key: "name", label: "Bộ sưu tập" }, { key: "season", label: "Mùa" }, { key: "year", label: "Năm" }, { key: "status", label: "Trạng thái" }];
const fields = [{ key: "code", label: "Mã", generated: true }, { key: "name", label: "Tên", required: true }, { key: "brandId", label: "Thương hiệu", type: "catalog", api: brandApi }, { key: "season", label: "Mùa" }, { key: "year", label: "Năm", type: "number" }, { key: "releaseDate", label: "Ngày ra mắt", type: "date" }, { key: "imageUrl", label: "Ảnh bộ sưu tập", type: "image" }, { key: "description", label: "Mô tả", type: "textarea" }];
const permissions = { create: "COLLECTION_CREATE", update: "COLLECTION_UPDATE", delete: "COLLECTION_DELETE" };
const normalizeCollection = (value) => ({ ...value, brandId: value.brandId || null, year: value.year === "" ? null : value.year, releaseDate: value.releaseDate || null });

export default function CollectionManagement() {
  return <ApiCatalogPage statusParam="status" statusOptions={[{ value: "INACTIVE", label: "Ngừng hoạt động" }, { value: "ALL", label: "Tất cả" }]} title="Bộ sưu tập" description="Bộ sưu tập Product dùng chung toàn chuỗi." icon={GalleryVerticalEnd} api={collectionApi} permissions={permissions} columns={columns} fields={fields} initialValues={{ status: "ACTIVE" }} normalize={normalizeCollection} renderDetails={(collection) => <CollectionProductsPanel collection={collection} />} />;
}
