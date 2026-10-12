import { AdminField, AdminFormSection } from "../common/AdminForm";
import { DISCOUNT_TYPES, PROMOTION_TYPES } from "./promotionOptions";

export default function PromotionBasicInfo({ mode, form, code, locked, onChange }) {
  const isView = mode === "view";
  const isCreate = mode === "create";

  return (
    <AdminFormSection title="Thông tin cơ bản">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <AdminField
          label="Mã khuyến mãi"
          disabled
          readOnly
          value={isCreate ? "Hệ thống tự sinh khi lưu" : code || ""}
        />
        <AdminField
          label="Tên khuyến mãi"
          required
          maxLength={255}
          disabled={isView}
          value={form.name}
          onChange={(event) => onChange("name", event.target.value)}
        />
        <AdminField
          as="select"
          label="Loại khuyến mãi"
          disabled={!isCreate}
          value={form.promotionType}
          onChange={(event) => onChange("promotionType", event.target.value)}
        >
          {PROMOTION_TYPES.map((type) => <option key={type.value} value={type.value}>{type.label}</option>)}
        </AdminField>
        <AdminField
          as="select"
          label="Kiểu giảm giá"
          disabled={isView || locked}
          value={form.discountType}
          onChange={(event) => onChange("discountType", event.target.value)}
        >
          {DISCOUNT_TYPES.map((type) => <option key={type.value} value={type.value}>{type.label}</option>)}
        </AdminField>
        <AdminField
          label={form.discountType === "PERCENT" ? "Giá trị giảm (%)" : "Giá trị giảm (đ)"}
          type="number"
          min="0"
          max={form.discountType === "PERCENT" ? 100 : undefined}
          step="any"
          required
          disabled={isView || locked}
          value={form.discountValue}
          onChange={(event) => onChange("discountValue", event.target.value)}
        />
        {form.discountType === "PERCENT" && (
          <AdminField
            label="Giảm tối đa (đ)"
            type="number"
            min="0"
            placeholder="Không giới hạn"
            disabled={isView}
            value={form.maxDiscount}
            onChange={(event) => onChange("maxDiscount", event.target.value)}
          />
        )}
      </div>
    </AdminFormSection>
  );
}
