import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import ProductCard from "./Product/ProductCard";
import { storefrontApi } from "../../api/storefront/storefrontApi";
import { buildProductQuery } from "../../pages/customer/storefrontLogic";

const COUNT = 4;

/** Top bán chạy: sản phẩm gắn tag "Best Seller" còn hàng, lấy từ API storefront. */
export default function BestSellerSection() {
  const [products, setProducts] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    storefrontApi.products(buildProductQuery({ search: "best seller", searchType: "tag", inStockOnly: true, size: COUNT }),
      { signal: controller.signal })
      .then((data) => setProducts(data.content || []))
      .catch(() => { if (!controller.signal.aborted) setProducts([]); });
    return () => controller.abort();
  }, []);

  if (products && products.length === 0) return null;

  return (
    <section className="home-section">
      <div className="home-section__heading">
        <div>
          <p>Được yêu thích nhất</p>
          <h2>Top bán chạy</h2>
        </div>
        <Link to="/products" className="home-section__link">Xem tất cả sản phẩm</Link>
      </div>

      <div
        className="
          flex gap-3
          overflow-x-auto
          snap-x snap-mandatory

          [&::-webkit-scrollbar]:hidden
          [scrollbar-width:none]

          sm:grid sm:grid-cols-2 sm:gap-5 sm:overflow-visible
          lg:grid-cols-4 lg:gap-6
        "
        aria-busy={!products || undefined}
      >
        {(products || Array.from({ length: COUNT }, (_, index) => ({ id: `loading-${index}`, loading: true }))).map((product) => (
          <div
            key={product.id}
            className="min-w-[68%] snap-start sm:min-w-0"
          >
            {product.loading
              ? <div className="customer-card product-card product-card--skeleton" aria-hidden="true"><div className="product-card__media" /><div className="product-card__content"><span /><span /></div></div>
              : <ProductCard product={product} />}
          </div>
        ))}
      </div>
    </section>
  );
}
