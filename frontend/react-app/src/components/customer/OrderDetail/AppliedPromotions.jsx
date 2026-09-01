import { BadgeCheck, TicketPercent } from "lucide-react";

const formatPrice = (value) => `${Number(value || 0).toLocaleString("vi-VN")} ₫`;
const getAmount = (promotion) => Number(promotion?.discount ?? promotion?.discount_amount ?? promotion?.amount ?? 0);

export default function AppliedPromotions({ promotions, discountTotal = 0 }) {
  const items = Array.isArray(promotions) ? promotions : [];
  const calculatedTotal = items.reduce((total, promotion) => total + getAmount(promotion), 0);
  const savedTotal = Number(discountTotal) || calculatedTotal;

  return (
    <section className="order-detail-promotions" aria-labelledby="order-promotions-title">
      <h3 id="order-promotions-title"><TicketPercent size={14} /> Ưu đãi đã sử dụng</h3>
      {items.length > 0 ? (
        <div className="order-detail-promotions__list">
          {items.map((promotion, index) => (
            <article key={promotion.id ?? promotion.code ?? index}>
              <BadgeCheck size={14} />
              <span><strong>{promotion.code || "ƯU ĐÃI"}</strong><small>{promotion.name || promotion.description || "Khuyến mãi đơn hàng"}</small></span>
              <b>-{formatPrice(getAmount(promotion))}</b>
            </article>
          ))}
        </div>
      ) : <p>Đơn hàng này không sử dụng khuyến mãi.</p>}

      {savedTotal > 0 && (
        <div className="order-detail-promotions__total"><span>Tổng tiết kiệm</span><strong>-{formatPrice(savedTotal)}</strong></div>
      )}
    </section>
  );
}
