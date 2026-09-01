import { ArrowUpRight } from "lucide-react";
import { Link } from "react-router-dom";

function BrandCard({ brand, index }) {
  return (
    <Link
      to={`/branddetail?id=${brand.id}`}
      className="home-brand-card"
      aria-label={`Khám phá thương hiệu ${brand.name}`}
    >
      <span className="home-brand-card__number">0{index + 1}</span>
      <span className="home-brand-card__logo">
        <img src={brand.logo} alt={`${brand.name} logo`} loading="lazy" />
      </span>
      <span className="home-brand-card__footer">
        <span>
          <strong>{brand.name}</strong>
          <small>{brand.category}</small>
        </span>
        <ArrowUpRight size={16} aria-hidden="true" />
      </span>
    </Link>
  );
}

export default function BrandsSection({ brands }) {
  return (
    <section className="home-section home-brands" aria-labelledby="brands-title">
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
        {brands.map((brand, index) => (
          <BrandCard key={brand.id} brand={brand} index={index} />
        ))}
      </div>
    </section>
  );
}
