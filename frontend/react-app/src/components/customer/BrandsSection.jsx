import { useEffect, useState } from "react";
import { ArrowUpRight } from "lucide-react";
import { Link } from "react-router-dom";
import { storefrontApi } from "../../api/storefront/storefrontApi";
import { productImage } from "../../pages/customer/storefrontLogic";

const COUNT = 6;

function BrandCard({ brand, index }) {
  return (
    <Link
      to={`/branddetail?id=${brand.id}`}
      className="home-brand-card"
      aria-label={`Khám phá thương hiệu ${brand.name}`}
    >
      <span className="home-brand-card__number">0{index + 1}</span>
      <span className="home-brand-card__logo">
        <img src={productImage(brand.logo, brand.name)} alt={`${brand.name} logo`} loading="lazy" />
      </span>
      <span className="home-brand-card__footer">
        <span>
          <strong>{brand.name}</strong>
          <small>{brand.productCount} sản phẩm · {brand.collectionCount} bộ sưu tập</small>
        </span>
        <ArrowUpRight size={16} aria-hidden="true" />
      </span>
    </Link>
  );
}

/** Hãng hợp tác trên trang chủ: các thương hiệu có nhiều sản phẩm đang bán nhất (API storefront). */
export default function BrandsSection() {
  const [brands, setBrands] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    storefrontApi.brands(new URLSearchParams({ page: "0", size: String(COUNT) }), { signal: controller.signal })
      .then((data) => setBrands(data.content || []))
      .catch(() => { if (!controller.signal.aborted) setBrands([]); });
    return () => controller.abort();
  }, []);

  if (brands && brands.length === 0) return null;
  return (
    <section className="home-section home-brands" aria-labelledby="brands-title" aria-busy={!brands || undefined}>
      <div className="home-section__heading">
        <div>
          <p>Selected maisons</p>
          <h2 id="brands-title">Hãng hợp tác</h2>
        </div>
        <Link to="/brand" className="home-section__link">
          Xem tất cả <ArrowUpRight size={15} />
        </Link>
      </div>

      <div className="home-brands__grid">
        {(brands || []).map((brand, index) => (
          <BrandCard key={brand.id} brand={brand} index={index} />
        ))}
      </div>
    </section>
  );
}
