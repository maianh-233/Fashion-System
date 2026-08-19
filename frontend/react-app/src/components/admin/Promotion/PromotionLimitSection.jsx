import { AdminField, AdminFormSection } from "../common/AdminForm";

export default function PromotionLimitSection({ mode, promotion }) {
  const isView = mode === "view";

  return (
    <AdminFormSection title="Điều kiện áp dụng">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <AdminField label="Đơn tối thiểu" type="number" min="0" disabled={isView} defaultValue={promotion?.min_order_value} />
        <AdminField label="Giảm tối đa" type="number" min="0" disabled={isView} defaultValue={promotion?.max_discount} />
        <AdminField label="Giới hạn lượt dùng" type="number" min="0" disabled={isView} defaultValue={promotion?.usage_limit} />
        <AdminField label="Lượt dùng mỗi người" type="number" min="0" disabled={isView} defaultValue={promotion?.usage_per_user} />
      </div>
    </AdminFormSection>
  );
}
