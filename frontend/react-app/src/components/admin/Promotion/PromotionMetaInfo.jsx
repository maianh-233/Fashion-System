import { AdminField, AdminFormSection } from "../common/AdminForm";

export default function PromotionMetaInfo({ createdAt, updatedAt }) {
  return (
    <AdminFormSection title="Thông tin hệ thống">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <AdminField label="Ngày tạo" disabled value={createdAt || ""} readOnly />
        <AdminField label="Cập nhật lần cuối" disabled value={updatedAt || ""} readOnly />
      </div>
    </AdminFormSection>
  );
}
