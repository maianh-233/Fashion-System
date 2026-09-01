import { ArrowUpRight } from "lucide-react";
import { Link } from "react-router-dom";

export default function BrandCard({ brand }) {
  if (!brand) return null;

  const { id, name, code, logo, description, status } = brand;

  return (
    <article className="customer-card brand-list-card group">
      <Link
        to={`/branddetail?id=${id}`}
        className="customer-card__link"
        aria-label={`Xem thương hiệu ${name}`}
      >
        <div className="customer-card__media brand-list-card__logo">
          <img src={logo} alt={name} loading="lazy" />
        </div>

        <div className="customer-card__body">
          <div className="customer-card__heading-row">
            <p className="customer-card__eyebrow">Maison · {code}</p>
            {status && (
              <span className="customer-card__status">
                {status === "active" ? "Đang hợp tác" : "Tạm ẩn"}
              </span>
            )}
          </div>
          <h3 className="customer-card__title">{name}</h3>
          {description && (
            <p className="customer-card__description">{description}</p>
          )}
          <div className="customer-card__footer">
            <span className="customer-card__meta">Khám phá thương hiệu</span>
            <span className="customer-card__arrow" aria-hidden="true">
              <ArrowUpRight size={15} />
            </span>
          </div>
        </div>
      </Link>
    </article>
  );
}
