import { CalendarDays, Layers3 } from "lucide-react";

export default function CollectionHero({ collection }) {
  return (
    <section className="collection-detail-hero">
      <img src={collection.banner} alt={collection.name} />
      <div className="collection-detail-hero__overlay" />
      <div className="customer-page__wide collection-detail-hero__inner">
        <div className="collection-detail-hero__panel">
          <p>{collection.season} · {collection.year}</p>
          <div className="collection-detail-hero__brand">
            <img src={collection.brand_logo} alt={`${collection.brand_name} logo`} />
            <span>{collection.brand_name}</span>
          </div>
          <h1>{collection.name}</h1>
          <p className="collection-detail-hero__description">{collection.description}</p>
          <div className="collection-detail-hero__meta">
            <span><Layers3 size={15} /> {collection.code}</span>
            <span><CalendarDays size={15} /> Ra mắt {new Date(collection.release_date).toLocaleDateString("vi-VN")}</span>
          </div>
        </div>
      </div>
    </section>
  );
}
