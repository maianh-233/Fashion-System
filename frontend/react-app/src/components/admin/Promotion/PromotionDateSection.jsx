import { AdminField, AdminFormSection } from "../common/AdminForm";

export default function PromotionDateSection({ mode, form, locked, onChange }) {
  const isView = mode === "view";

  return (
    <AdminFormSection title="Thời gian áp dụng">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <AdminField
          label="Ngày bắt đầu"
          type="datetime-local"
          required
          disabled={isView || locked}
          value={form.startDate}
          onChange={(event) => onChange("startDate", event.target.value)}
        />
        <AdminField
          label="Ngày kết thúc"
          type="datetime-local"
          required
          min={form.startDate || undefined}
          disabled={isView}
          value={form.endDate}
          onChange={(event) => onChange("endDate", event.target.value)}
        />
      </div>
    </AdminFormSection>
  );
}
