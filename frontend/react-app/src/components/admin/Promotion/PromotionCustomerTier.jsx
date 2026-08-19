import { AdminField, AdminFormSection } from "../common/AdminForm";

const tiers = ["Bronze", "Silver", "Gold", "Platinum"];

export default function PromotionCustomerTier({ mode, promotion }) {
  return (
    <AdminFormSection title="Hạng khách hàng áp dụng">
      <AdminField as="select" label="Hạng thành viên" disabled={mode === "view"} defaultValue={promotion?.tier || ""}>
        <option value="">Tất cả khách hàng</option>
        {tiers.map((item) => <option key={item} value={item}>{item}</option>)}
      </AdminField>
    </AdminFormSection>
  );
}
