import { AdminFormSection } from "../common/AdminForm";

export default function PromotionCustomerTier({ mode, tiers, selectedIds, loading, onChange }) {
  const isView = mode === "view";
  const toggle = (id) => onChange(selectedIds.includes(id)
    ? selectedIds.filter((value) => value !== id)
    : [...selectedIds, id]);

  return (
    <AdminFormSection
      title="Hạng khách hàng áp dụng"
      description="Không chọn hạng nào nghĩa là áp dụng cho tất cả khách hàng."
    >
      {loading ? (
        <p className="text-sm text-zinc-400">Đang tải hạng khách hàng...</p>
      ) : tiers.length === 0 ? (
        <p className="text-sm text-zinc-400">Chưa có hạng khách hàng nào.</p>
      ) : (
        <div className="flex flex-wrap gap-3">
          {tiers.map((tier) => (
            <label
              key={tier.id}
              className={`flex items-center gap-2 rounded-xl border px-3 py-2 text-sm ${
                selectedIds.includes(tier.id) ? "border-amber-400 text-amber-300" : "border-zinc-700 text-zinc-300"
              } ${isView ? "opacity-80" : "cursor-pointer"}`}
            >
              <input
                type="checkbox"
                disabled={isView}
                checked={selectedIds.includes(tier.id)}
                onChange={() => toggle(tier.id)}
              />
              {tier.name}
            </label>
          ))}
        </div>
      )}
    </AdminFormSection>
  );
}
