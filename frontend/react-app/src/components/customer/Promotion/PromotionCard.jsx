import { Calendar, TicketPercent, DollarSign, Truck } from "lucide-react";

export default function PromotionCard({ promotion }) {
  if (!promotion) return null;

  const {
    code,
    name,
    discount_type,
    discount_value,
    start_date,
    end_date,
    min_order_value,
    active,
  } = promotion;

  /* ===== Helpers ===== */
  const formatDate = (date) =>
    new Date(date).toLocaleDateString("vi-VN");

  const formatMoney = (value) =>
    Number(value).toLocaleString("vi-VN");

  const renderDiscount = () => {
    switch (discount_type) {
      case "percent":
        return (
          <span>
            <TicketPercent size={16} />
            Giảm {discount_value}%
          </span>
        );
      case "cash":
        return (
          <span>
            <DollarSign size={16} />
            Giảm {formatMoney(discount_value)}đ
          </span>
        );
      case "freeship":
        return (
          <span>
            <Truck size={16} />
            Freeship
          </span>
        );
      default:
        return null;
    }
  };

  return (
    <article className="customer-card promotion-card">
      <div className="customer-card__body">
        <div className="customer-card__heading-row">
          <p className="customer-card__eyebrow">Đặc quyền · {code}</p>
          <span className="customer-card__status">
          {active ? "Đang áp dụng" : "Ngừng hoạt động"}
          </span>
        </div>
        <h3 className="customer-card__title">{name}</h3>
        <div className="promotion-card__value">{renderDiscount()}</div>
        <p className="customer-card__description">
          Áp dụng cho đơn từ <strong>{formatMoney(min_order_value)}đ</strong>
        </p>
        <div className="customer-card__footer promotion-card__period">
          <span className="customer-card__meta">
            <Calendar size={14} />
            {formatDate(start_date)} – {formatDate(end_date)}
          </span>
          <span className="promotion-card__code">{code}</span>
        </div>
      </div>
    </article>
  );
}
