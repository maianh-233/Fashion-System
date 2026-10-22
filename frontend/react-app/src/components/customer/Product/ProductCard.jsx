import { ArrowUpRight } from "lucide-react";
import { Link } from "react-router-dom";
import { productImage, tagLabel } from "../../../pages/customer/storefrontLogic";

/**
 * Thẻ sản phẩm. Nhận dữ liệu API storefront (imageUrl, minPrice, minOriginalPrice, inStock, slug) và vẫn hiểu
 * dạng cũ (thumbnail, min_price) của các trang chưa nối API.
 */
function ProductCard({ product }) {
  const { name } = product;
  const price = product.minPrice ?? product.min_price;
  const original = product.minOriginalPrice;
  const discounted = product.onSale && original != null && Number(original) > Number(price);
  const soldOut = product.inStock === false;
  const tags = soldOut ? ["Hết hàng", ...(product.tags || [])] : product.tags || [];
  const image = productImage(product.imageUrl ?? product.thumbnail, name);

  return (
    <article className={`customer-card product-card group ${soldOut ? "is-sold-out" : ""}`}>
      <Link
        to={`/productdetail?id=${encodeURIComponent(product.slug || product.id)}`}
        className="product-card__link"
        aria-label={`Xem chi tiết ${name}`}
      >
        <div className="product-card__media">
          <img
            src={image}
            alt={name}
            loading="lazy"
            className="product-card__image"
          />

          {tags.length > 0 && (
            <div className="product-card__tags">
              {tags.slice(0, 2).map((tag) => (
                <span key={tag} className="product-card__tag">
                  {tagLabel(tag)}
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
          {product.brandName && <p className="product-card__brand">{product.brandName}</p>}
          <h3 className="product-card__name">{name}</h3>

          <div className="product-card__footer">
            {price != null && (
              <p className="product-card__price">
                <span className="product-card__price-prefix">Từ </span>
                {Number(price).toLocaleString("vi-VN")} ₫
                {discounted && <del className="product-card__price-original">{Number(original).toLocaleString("vi-VN")} ₫</del>}
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
