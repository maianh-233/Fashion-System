import { AdminField, AdminFormSection } from "../common/AdminForm";

export default function PromotionLimitSection({ mode, form, onChange }) {
  const isView = mode === "view";
  const type = form.promotionType;
  const numberField = (field, label, placeholder) => (
    <AdminField
      label={label}
      type="number"
      min="0"
      placeholder={placeholder}
      disabled={isView}
      value={form[field]}
      onChange={(event) => onChange(field, event.target.value)}
    />
  );

  return (
    <AdminFormSection
      title="Điều kiện áp dụng"
      description="Các giới hạn để trống nghĩa là không giới hạn."
    >
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        {type === "POINT_REDEMPTION" && (
          <AdminField
            label="Số điểm cần đổi"
            type="number"
            min="1"
            required
            disabled={isView}
            value={form.pointsRequired}
            onChange={(event) => onChange("pointsRequired", event.target.value)}
          />
        )}
        {type !== "PRODUCT" && numberField("minOrderValue", "Đơn hàng tối thiểu (đ)", "Không yêu cầu")}
        {numberField("usageLimit", type === "POINT_REDEMPTION" ? "Tổng lượt đổi tối đa" : "Tổng lượt dùng tối đa",
          "Không giới hạn")}
        {type !== "POINT_REDEMPTION" && numberField("usagePerUser", "Lượt dùng mỗi khách", "Không giới hạn")}
      </div>
      {type === "POINT_REDEMPTION" && (
        <p className="mt-3 text-xs text-zinc-400">
          Khách đủ điểm được đổi không giới hạn số lần; mỗi lần đổi sẽ trừ điểm tích lũy của khách.
        </p>
      )}
    </AdminFormSection>
  );
}
