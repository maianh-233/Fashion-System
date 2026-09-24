import { Truck } from "lucide-react";
import { supplierApi } from "../../api/catalogApi";
import ApiCatalogPage from "../../components/admin/catalog/ApiCatalogPage";
import { isSupplierRestorable } from "./supplierPageLogic";

const statusLabels = {
  ACTIVE: "Đang hợp tác",
  INACTIVE: "Ngừng hợp tác",
  DELETED: "Đã xóa mềm",
};

const columns = [
  { key: "code", label: "Mã NCC" },
  { key: "name", label: "Tên nhà cung cấp" },
  { key: "contactName", label: "Người liên hệ" },
  { key: "email", label: "Email" },
  { key: "phone", label: "Số điện thoại" },
  { key: "status", label: "Trạng thái", render: (row) => statusLabels[row.status] || row.status },
];

const fields = [
  { key: "code", label: "Mã nhà cung cấp", generated: true },
  { key: "name", label: "Tên nhà cung cấp", required: true },
  { key: "contactName", label: "Người liên hệ" },
  { key: "email", label: "Email", type: "email" },
  { key: "phone", label: "Số điện thoại", type: "tel" },
  { key: "address", label: "Địa chỉ", type: "textarea" },
  { key: "status", label: "Trạng thái", type: "select", hideOnCreate: true, options: [
    { value: "ACTIVE", label: "Đang hợp tác" },
    { value: "INACTIVE", label: "Ngừng hợp tác" },
  ] },
];

const permissions = {
  create: "SUPPLIER_CREATE",
  update: "SUPPLIER_UPDATE",
  delete: "SUPPLIER_DELETE",
};

export default function SupplierManagement() {
  return <ApiCatalogPage
    title="Nhà cung cấp"
    description="Quản lý đối tác cung ứng, người liên hệ và trạng thái hợp tác kinh doanh."
    icon={Truck}
    api={supplierApi}
    permissions={permissions}
    columns={columns}
    fields={fields}
    statusParam="status"
    statusOptions={[
      { value: "INACTIVE", label: "Ngừng hợp tác" },
      { value: "DELETED", label: "Đã xóa mềm" },
      { value: "ALL", label: "Tất cả" },
    ]}
    initialValues={{ status: "ACTIVE" }}
    sort="name,asc"
    dialogMaxWidth="max-w-3xl"
    isRestorable={isSupplierRestorable}
  />;
}
