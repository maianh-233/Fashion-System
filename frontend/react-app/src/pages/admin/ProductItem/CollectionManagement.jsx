import { GalleryVerticalEnd } from "lucide-react";
import ApiCatalogPage from "../../../components/admin/catalog/ApiCatalogPage";
import { collectionApi } from "../../../hooks/catalogApi";

const columns = [{ key: "code", label: "Mã" }, { key: "name", label: "Bộ sưu tập" }, { key: "season", label: "Mùa" }, { key: "year", label: "Năm" }, { key: "status", label: "Trạng thái" }];
const fields = [{ key: "code", label: "Mã", required: true }, { key: "name", label: "Tên", required: true }, { key: "brandId", label: "UUID thương hiệu" }, { key: "season", label: "Mùa" }, { key: "year", label: "Năm", type: "number" }, { key: "releaseDate", label: "Ngày ra mắt", type: "date" }, { key: "status", label: "Trạng thái", type: "select", options: [{ value: "ACTIVE", label: "Hoạt động" }, { value: "DRAFT", label: "Bản nháp" }, { value: "INACTIVE", label: "Ngừng hoạt động" }] }, { key: "description", label: "Mô tả", type: "textarea" }];
const permissions = { create: "COLLECTION_CREATE", update: "COLLECTION_UPDATE", delete: "COLLECTION_DELETE" };
const normalizeCollection = (value) => ({ ...value, brandId: value.brandId || null, year: value.year === "" ? null : value.year, releaseDate: value.releaseDate || null });

export default function CollectionManagement() {
  return <ApiCatalogPage title="Bộ sưu tập" description="Bộ sưu tập Product dùng chung toàn chuỗi." icon={GalleryVerticalEnd} api={collectionApi} permissions={permissions} columns={columns} fields={fields} initialValues={{ status: "ACTIVE" }} normalize={normalizeCollection} />;
}
