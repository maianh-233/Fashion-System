import PromoInput from "./PromoInput";

export default function OrderSummary({
  subtotal,
  total,
  appliedPromos,
  onApplyPromo,
  onRemovePromo,
}) {
  return (
    <div
      className="
        w-full lg:w-[420px]

        sticky lg:static
        bottom-0
        z-40

        bg-zinc-900
        border-t border-zinc-800
        lg:border
        lg:rounded-2xl

        p-4 sm:p-6
        shadow-[0_-4px_20px_rgba(0,0,0,0.5)]
        lg:shadow-lg
      "
    >
      <h3 className="text-lg sm:text-xl font-semibold mb-4">
        Thông tin thanh toán
      </h3>

      {/* PROMO INPUT */}
      <div className="mb-4">
        <PromoInput
          onApply={onApplyPromo}
          disabled={appliedPromos.length >= 3}
        />
      </div>

      {/* PROMO TAGS */}
      <div className="space-y-2 mb-4">
        {appliedPromos.map((p) => (
          <div
            key={p.code}
            className="
              flex justify-between items-center
              bg-zinc-800 rounded-xl
              px-3 py-2 text-sm
            "
          >
            <span>{p.code}</span>
            <button
              onClick={() => onRemovePromo(p.code)}
              className="text-red-400"
            >
              ✕
            </button>
          </div>
        ))}
      </div>

      {/* TOTAL */}
      <div className="space-y-2 text-sm sm:text-base text-zinc-400">
        <div className="flex justify-between">
          <span>Tạm tính</span>
          <span>{subtotal.toLocaleString("vi-VN")} ₫</span>
        </div>

        {appliedPromos.map((p) => (
          <div
            key={p.code}
            className="flex justify-between text-emerald-400"
          >
            <span>Giảm ({p.code})</span>
            <span>-{p.amount.toLocaleString("vi-VN")} ₫</span>
          </div>
        ))}

        <hr className="border-zinc-700 my-2" />

        <div className="flex justify-between text-lg font-semibold">
          <span>Tổng</span>
          <span className="text-amber-400">
            {total.toLocaleString("vi-VN")} ₫
          </span>
        </div>
      </div>

      <button
        className="
          mt-4
          w-full
          bg-amber-400 hover:bg-amber-500
          text-black
          py-3 sm:py-4
          rounded-xl
          font-semibold
        "
      >
        Thanh toán
      </button>
    </div>
  );
}