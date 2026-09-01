import { ArrowDown, MapPin } from "lucide-react";

export default function BrandHero({ brand }) {
  return (
    <section className="brand-detail-hero">
      <div className="brand-detail-hero__art" aria-hidden="true">
        <span />
        <span />
      </div>
      <div className="customer-page__wide brand-detail-hero__inner">
        <div className="brand-detail-hero__logo">
          <img src={brand.logo} alt={`${brand.name} logo`} />
        </div>
        <div className="brand-detail-hero__copy">
          <p>Featured maison</p>
          <h1>{brand.name}</h1>
          <span><MapPin size={14} /> {brand.origin}</span>
          <p className="brand-detail-hero__description">{brand.description}</p>
          <a href="#collections">Khám phá bộ sưu tập <ArrowDown size={15} /></a>
        </div>
      </div>
    </section>
  );
}
