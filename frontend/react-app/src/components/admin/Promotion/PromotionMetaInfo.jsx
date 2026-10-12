import { AdminField, AdminFormSection } from "../common/AdminForm";
import { PROMOTION_STATUSES, formatDateTime, labelOf } from "./promotionOptions";

export default function PromotionMetaInfo({ promotion }) {
  return (
    <AdminFormSection title="Thông tin hệ thống">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <AdminField label="Trạng thái" disabled readOnly value={labelOf(PROMOTION_STATUSES, promotion?.status)} />
        <AdminField label="Số lượt đã dùng / đã đổi" disabled readOnly value={promotion?.usedCount ?? 0} />
        <AdminField label="Ngày tạo" disabled readOnly value={formatDateTime(promotion?.createdAt)} />
        <AdminField label="Cập nhật lần cuối" disabled readOnly value={formatDateTime(promotion?.updatedAt)} />
      </div>
    </AdminFormSection>
  );
}
