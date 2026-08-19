import { BadgeCheck, TicketPercent } from "lucide-react";

const formatCurrency = (value) =>
  `${Number(value || 0).toLocaleString("vi-VN")} ₫`;

const getDiscountAmount = (promotion) =>
  Number(
    promotion?.discount ??
      promotion?.discount_amount ??
      promotion?.amount ??
      0,
  );

export default function AppliedPromotions({ promotions, discountTotal = 0 }) {
  const appliedPromotions = Array.isArray(promotions) ? promotions : [];
  const calculatedTotal = appliedPromotions.reduce(
    (total, promotion) => total + getDiscountAmount(promotion),
    0,
  );
  const savedTotal = Number(discountTotal) || calculatedTotal;

  return (
    <section
      className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900"
      aria-labelledby="applied-promotions-title"
    >
      <div className="flex items-center justify-between gap-3 border-b border-zinc-800 px-4 py-4 sm:px-5">
        <div className="flex min-w-0 items-center gap-3">
          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-400">
            <TicketPercent size={20} />
          </span>
          <div className="min-w-0">
            <h2
              id="applied-promotions-title"
              className="font-semibold text-zinc-100"
            >
              Khuyến mãi đã sử dụng
            </h2>
            <p className="text-xs text-zinc-400">
              {appliedPromotions.length} ưu đãi được áp dụng
            </p>
          </div>
        </div>
        <BadgeCheck className="shrink-0 text-emerald-400" size={22} />
      </div>

      {appliedPromotions.length > 0 ? (
        <div className="divide-y divide-zinc-800">
          {appliedPromotions.map((promotion, index) => (
            <article
              key={promotion.id ?? promotion.code ?? index}
              className="flex flex-col gap-2 px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:gap-4 sm:px-5"
            >
              <div className="min-w-0">
                <p className="break-all text-sm font-semibold text-emerald-400">
                  {promotion.code || "ƯU ĐÃI"}
                </p>
                <p className="mt-1 text-sm leading-relaxed text-zinc-400">
                  {promotion.name || promotion.description || "Khuyến mãi đơn hàng"}
                </p>
              </div>
              <p className="shrink-0 font-semibold text-emerald-400 sm:text-right">
                -{formatCurrency(getDiscountAmount(promotion))}
              </p>
            </article>
          ))}
        </div>
      ) : (
        <p className="px-4 py-5 text-sm text-zinc-400 sm:px-5">
          Đơn hàng này không sử dụng khuyến mãi.
        </p>
      )}

      {savedTotal > 0 && (
        <div className="flex items-center justify-between gap-4 border-t border-zinc-800 bg-emerald-500/5 px-4 py-4 sm:px-5">
          <span className="text-sm text-zinc-300">Tổng đã giảm</span>
          <strong className="shrink-0 text-emerald-400">
            -{formatCurrency(savedTotal)}
          </strong>
        </div>
      )}
    </section>
  );
}
