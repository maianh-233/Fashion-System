import { Tag } from "lucide-react";

const formatPrice = (value) => `${Number(value).toLocaleString("vi-VN")} ₫`;

export default function Promotions({ promotions = [] }) {
  if (promotions.length === 0) return null;

  return (
    <div className="checkout-promotions">
      <h3><Tag size={14} /> Ưu đãi đã áp dụng</h3>
      <div>
        {promotions.map((promotion) => (
          <article key={promotion.code}>
            <span><strong>{promotion.code}</strong><small>{promotion.name}</small></span>
            <b>-{formatPrice(promotion.discount)}</b>
          </article>
        ))}
      </div>
    </div>
  );
}
