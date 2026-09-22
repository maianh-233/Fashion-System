import { BadgeCheck } from "lucide-react";
import ApiCatalogPage from "../../../components/admin/catalog/ApiCatalogPage";
import { brandApi } from "../../../hooks/catalogApi";

const columns = [{ key: "logo", label: "Logo", render: (row) => row.logo ? <img src={row.logo} alt={row.name} className="h-10 w-10 rounded-lg object-cover" /> : "—" }, { key: "code", label: "Mã" }, { key: "name", label: "Thương hiệu" }, { key: "status", label: "Trạng thái" }];
const fields = [{ key: "code", label: "Mã", generated: true }, { key: "name", label: "Tên", required: true }, { key: "logo", label: "Logo", type: "image" }, { key: "description", label: "Mô tả", type: "textarea" }];
const permissions = { create: "BRAND_CREATE", update: "BRAND_UPDATE", delete: "BRAND_DELETE" };

export default function BrandManagement() {
  return <ApiCatalogPage statusParam="status" statusOptions={[{ value: "INACTIVE", label: "Ngừng hoạt động" }, { value: "ALL", label: "Tất cả" }]} title="Thương hiệu" description="Master data thương hiệu dùng chung toàn chuỗi." icon={BadgeCheck} api={brandApi} permissions={permissions} columns={columns} fields={fields} initialValues={{ status: "ACTIVE" }} />;
}
