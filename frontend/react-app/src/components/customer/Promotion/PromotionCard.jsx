import { useState } from "react";
import { Calendar, Check, Copy, Gift, ShoppingBag, TicketPercent } from "lucide-react";
import { Link } from "react-router-dom";
import {
  PROMOTION_TYPE_LABELS, formatDate, promotionConditions, promotionStatus, promotionValueLabel,
} from "../../../pages/customer/storefrontLogic";

const TYPE_ICONS = { ORDER: TicketPercent, PRODUCT: ShoppingBag, POINT_REDEMPTION: Gift };

/** Thẻ khuyến mãi công khai (dữ liệu API storefront). */
export default function PromotionCard({ promotion }) {
  const [copied, setCopied] = useState(false);
  if (!promotion) return null;

  const { code, name, promotionType, startDate, endDate } = promotion;
  const status = promotionStatus(promotion);
  const Icon = TYPE_ICONS[promotionType] || TicketPercent;
  const usage = promotionType === "POINT_REDEMPTION"
    ? "Đổi điểm để nhận voucher, sau đó chọn voucher khi thanh toán."
    : "Nhập hoặc chọn mã khi thanh toán.";

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(code);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1500);
    } catch {
      setCopied(false);
    }
  };

  return (
    <article className={`customer-card promotion-card promotion-card--${status.key}`}>
      <div className="customer-card__body">
        <div className="customer-card__heading-row">
          <p className="customer-card__eyebrow">{PROMOTION_TYPE_LABELS[promotionType] || promotionType}</p>
          <span className="customer-card__status">{status.label}</span>
        </div>
        <h3 className="customer-card__title">{name}</h3>
        <div className="promotion-card__value">
          <span><Icon size={16} /> {promotionValueLabel(promotion)}</span>
        </div>
        <ul className="promotion-card__conditions">
          {promotionConditions(promotion).map((line) => <li key={line}>{line}</li>)}
          <li>{usage}</li>
        </ul>
        {promotionType === "PRODUCT" && promotion.productCount > 0 && (
          <Link className="promotion-card__link" to={`/products?promotionId=${promotion.id}&promotion=${encodeURIComponent(code)}`}>
            Xem sản phẩm áp dụng
          </Link>
        )}
        <div className="customer-card__footer promotion-card__period">
          <span className="customer-card__meta">
            <Calendar size={14} />
            {formatDate(startDate)} – {formatDate(endDate)}
          </span>
          <button type="button" className="promotion-card__code" onClick={copy} aria-label={`Sao chép mã ${code}`}
            title="Sao chép mã">
            {code} {copied ? <Check size={13} /> : <Copy size={13} />}
          </button>
        </div>
      </div>
    </article>
  );
}
