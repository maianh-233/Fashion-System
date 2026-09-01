import { ArrowUpRight } from "lucide-react";
import { Link } from "react-router-dom";

export default function CollectionCard({ collection }) {
  if (!collection) return null;

  const { id, name, brand, season, year, cover_image } = collection;

  return (
    <article className="customer-card collection-card group">
      <Link
        to={`/collectiondetail?id=${id}`}
        className="customer-card__link"
        aria-label={`Xem bộ sưu tập ${name}`}
      >
        <div className="customer-card__media collection-card__media">
          <img
            src={cover_image || "/placeholder-collection.jpg"}
            alt={name}
            loading="lazy"
          />
        </div>

        <div className="customer-card__body collection-card__content">
          <p className="customer-card__eyebrow collection-card__brand">
            {brand || "Lunaria selection"}
          </p>
          <h2 className="customer-card__title collection-card__title">{name}</h2>
          <div className="customer-card__footer">
            <p className="customer-card__meta collection-card__meta">
              {[season, year].filter(Boolean).join(" · ")}
            </p>
            <span className="customer-card__arrow" aria-hidden="true">
              <ArrowUpRight size={15} />
            </span>
          </div>
        </div>
      </Link>
    </article>
  );
}
