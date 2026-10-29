import { useState } from "react";
import { FolderTree, Ruler } from "lucide-react";
import Button from "../../../components/common/Button";
import ApiCatalogPage from "../../../components/admin/catalog/ApiCatalogPage";
import SizeChartDialog from "../../../components/admin/catalog/SizeChartDialog";
import { categoryApi } from "../../../api/admin/catalogApi";

const columns = [{ key: "code", label: "Mã" }, { key: "name", label: "Danh mục" }, { key: "parentId", label: "Danh mục cha" }, { key: "active", label: "Trạng thái", render: (row) => row.active ? "Hoạt động" : "Ngừng hoạt động" }];
const fields = [{ key: "name", label: "Tên", required: true, fullWidth: true }, { key: "code", label: "Mã", generated: true }, { key: "parentId", label: "Danh mục cha", type: "catalog", api: categoryApi }];
const permissions = { create: "CATEGORY_CREATE", update: "CATEGORY_UPDATE", delete: "CATEGORY_DELETE" };
const normalizeCategory = (value) => ({ ...value, parentId: value.parentId || null });

export default function CategoryManagement() {
  const [sizeChartFor, setSizeChartFor] = useState(null);
  const renderActions = (row) => (
    <Button permission="CATEGORY_VIEW" type="button" onClick={() => setSizeChartFor(row)} title="Bảng size"
      aria-label={`Bảng size của ${row.name}`} className="text-violet-400"><Ruler size={17} /></Button>
  );
  return (
    <>
      <ApiCatalogPage statusParam="active" statusOptions={[{ value: "false", label: "Ngừng hoạt động" }]} title="Danh mục" description="Cấu trúc danh mục Product dùng chung toàn chuỗi." icon={FolderTree} api={categoryApi} permissions={permissions} columns={columns} fields={fields} normalize={normalizeCategory} renderActions={renderActions} />
      {sizeChartFor && <SizeChartDialog category={sizeChartFor} onClose={() => setSizeChartFor(null)} />}
    </>
  );
}
