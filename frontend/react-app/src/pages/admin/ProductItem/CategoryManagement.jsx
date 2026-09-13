import { FolderTree } from "lucide-react";
import ApiCatalogPage from "../../../components/admin/catalog/ApiCatalogPage";
import { categoryApi } from "../../../hooks/catalogApi";

const columns = [{ key: "code", label: "Mã" }, { key: "name", label: "Danh mục" }, { key: "parentId", label: "Danh mục cha" }];
const fields = [{ key: "code", label: "Mã", required: true }, { key: "name", label: "Tên", required: true }, { key: "parentId", label: "UUID danh mục cha" }];
const permissions = { create: "CATEGORY_CREATE", update: "CATEGORY_UPDATE", delete: "CATEGORY_DELETE" };
const normalizeCategory = (value) => ({ ...value, parentId: value.parentId || null });

export default function CategoryManagement() {
  return <ApiCatalogPage title="Danh mục" description="Cấu trúc danh mục Product dùng chung toàn chuỗi." icon={FolderTree} api={categoryApi} permissions={permissions} columns={columns} fields={fields} normalize={normalizeCategory} />;
}
