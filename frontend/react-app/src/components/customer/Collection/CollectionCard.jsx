import { ArrowUpRight } from "lucide-react";
import { Link } from "react-router-dom";
import { productImage, seasonLabel } from "../../../pages/customer/storefrontLogic";

/** Thẻ bộ sưu tập (dữ liệu API storefront). */
export default function CollectionCard({ collection }) {
  if (!collection) return null;

  const { id, name, brand, season, year, imageUrl, productCount } = collection;

  return (
    <article className="customer-card collection-card group">
      <Link
        to={`/collectiondetail?id=${id}`}
        className="customer-card__link"
        aria-label={`Xem bộ sưu tập ${name}`}
      >
        <div className="customer-card__media collection-card__media">
          <img
            src={productImage(imageUrl, name)}
            alt={name}
            loading="lazy"
          />
        </div>

        <div className="customer-card__body collection-card__content">
          <p className="customer-card__eyebrow collection-card__brand">
            {brand?.name || "Lunaria selection"}
          </p>
          <h2 className="customer-card__title collection-card__title">{name}</h2>
          <div className="customer-card__footer">
            <p className="customer-card__meta collection-card__meta">
              {[seasonLabel(season, year), productCount != null && `${productCount} sản phẩm`].filter(Boolean).join(" · ")}
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
