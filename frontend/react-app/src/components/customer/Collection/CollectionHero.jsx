import { CalendarDays, Layers3, Package } from "lucide-react";
import { Link } from "react-router-dom";
import { formatDate, productImage, seasonLabel } from "../../../pages/customer/storefrontLogic";

export default function CollectionHero({ collection }) {
  const brandName = collection.brand?.name;
  return (
    <section className="collection-detail-hero">
      <img src={productImage(collection.imageUrl, collection.name)} alt={collection.name} />
      <div className="collection-detail-hero__overlay" />
      <div className="customer-page__wide collection-detail-hero__inner">
        <div className="collection-detail-hero__panel">
          <p>{seasonLabel(collection.season, collection.year)}</p>
          {brandName && (
            <Link to={`/branddetail?id=${collection.brand.id}`} className="collection-detail-hero__brand">
              <img src={productImage(collection.brandLogo, brandName)} alt={`${brandName} logo`} />
              <span>{brandName}</span>
            </Link>
          )}
          <h1>{collection.name}</h1>
          {collection.description && <p className="collection-detail-hero__description">{collection.description}</p>}
          <div className="collection-detail-hero__meta">
            <span><Layers3 size={15} /> {collection.code}</span>
            {collection.releaseDate && <span><CalendarDays size={15} /> Ra mắt {formatDate(collection.releaseDate)}</span>}
            <span><Package size={15} /> {collection.productCount} sản phẩm</span>
          </div>
        </div>
      </div>
    </section>
  );
}
