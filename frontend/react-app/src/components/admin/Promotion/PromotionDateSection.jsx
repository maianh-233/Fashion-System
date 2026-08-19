import { AdminField, AdminFormSection } from "../common/AdminForm";

export default function PromotionDateSection({ mode, promotion }) {
  const isView = mode === "view";

  return (
    <AdminFormSection title="Thời gian áp dụng">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <AdminField label="Ngày bắt đầu" type="datetime-local" disabled={isView} defaultValue={promotion?.start_date} />
        <AdminField label="Ngày kết thúc" type="datetime-local" disabled={isView} defaultValue={promotion?.end_date} />
      </div>
    </AdminFormSection>
  );
}
