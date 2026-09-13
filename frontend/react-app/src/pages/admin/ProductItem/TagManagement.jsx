import { Tags } from "lucide-react";
import ApiCatalogPage from "../../../components/admin/catalog/ApiCatalogPage";
import { tagApi } from "../../../hooks/catalogApi";

const columns = [{ key: "name", label: "Tên tag" }, { key: "createdAt", label: "Ngày tạo", render: (row) => row.createdAt ? new Date(row.createdAt).toLocaleString("vi-VN") : "—" }];
const fields = [{ key: "name", label: "Tên tag", required: true }];
const permissions = { create: "TAG_CREATE", update: "TAG_UPDATE", delete: "TAG_DELETE" };

export default function TagManagement() {
  return <ApiCatalogPage title="Product Tag" description="Tag phân loại Product dùng chung toàn chuỗi." icon={Tags} api={tagApi} permissions={permissions} columns={columns} fields={fields} />;
}
