import { BadgeCheck } from "lucide-react";
import ApiCatalogPage from "../../../components/admin/catalog/ApiCatalogPage";
import { brandApi } from "../../../hooks/catalogApi";

const columns = [{ key: "code", label: "Mã" }, { key: "name", label: "Thương hiệu" }, { key: "status", label: "Trạng thái" }];
const fields = [{ key: "code", label: "Mã", required: true }, { key: "name", label: "Tên", required: true }, { key: "status", label: "Trạng thái", type: "select", options: [{ value: "ACTIVE", label: "Hoạt động" }, { value: "INACTIVE", label: "Ngừng hoạt động" }] }, { key: "description", label: "Mô tả", type: "textarea" }];
const permissions = { create: "BRAND_CREATE", update: "BRAND_UPDATE", delete: "BRAND_DELETE" };

export default function BrandManagement() {
  return <ApiCatalogPage title="Thương hiệu" description="Master data thương hiệu dùng chung toàn chuỗi." icon={BadgeCheck} api={brandApi} permissions={permissions} columns={columns} fields={fields} initialValues={{ status: "ACTIVE" }} />;
}
