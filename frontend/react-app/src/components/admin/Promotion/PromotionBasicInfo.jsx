import { AdminField, AdminFormSection } from "../common/AdminForm";

export default function PromotionBasicInfo({ mode, promotion }) {
  const isView = mode === "view";

  return (
    <AdminFormSection title="Thông tin cơ bản">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <AdminField label="Mã khuyến mãi" disabled={isView} defaultValue={promotion?.code} />
        <AdminField label="Tên khuyến mãi" disabled={isView} defaultValue={promotion?.name} />
        <AdminField
          label="Loại giảm giá"
          as="select"
          disabled={isView}
          defaultValue={promotion?.discount_type || "PERCENT"}
        >
          <option value="PERCENT">Theo phần trăm</option>
          <option value="FIXED">Theo số tiền</option>
        </AdminField>
        <AdminField label="Giá trị giảm" type="number" min="0" disabled={isView} defaultValue={promotion?.discount_value} />
      </div>
    </AdminFormSection>
  );
}
