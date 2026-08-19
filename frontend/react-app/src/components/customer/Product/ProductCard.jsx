import { ArrowUpRight } from "lucide-react";
import { Link } from "react-router-dom";

function ProductCard({ product }) {
  const { name, tags, thumbnail, min_price } = product;

  return (
    <article className="product-card group">
      <Link
        to={`/productdetail?id=${product.id}`}
        className="product-card__link"
        aria-label={`Xem chi tiết ${name}`}
      >
        <div className="product-card__media">
          <img
            src={thumbnail}
            alt={name}
            loading="lazy"
            className="product-card__image"
          />

          {tags?.length > 0 && (
            <div className="product-card__tags">
              {tags.slice(0, 2).map((tag) => (
                <span key={tag} className="product-card__tag">
                  {tag}
                </span>
              ))}
            </div>
          )}

          <span className="product-card__desktop-cta" aria-hidden="true">
            Xem chi tiết
            <ArrowUpRight size={14} />
          </span>
        </div>

        <div className="product-card__content">
          <h3 className="product-card__name">{name}</h3>

          <div className="product-card__footer">
            {min_price != null && (
              <p className="product-card__price">
                <span className="product-card__price-prefix">Từ </span>
                {Number(min_price).toLocaleString("vi-VN")} ₫
              </p>
            )}

            <span className="product-card__mobile-arrow" aria-hidden="true">
              <ArrowUpRight size={16} />
            </span>
          </div>
        </div>
      </Link>
    </article>
  );
}

export default ProductCard;
