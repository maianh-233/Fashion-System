import { ArrowDown } from "lucide-react";
import { productImage } from "../../../pages/customer/storefrontLogic";

export default function BrandHero({ brand }) {
  return (
    <section className="brand-detail-hero">
      <div className="brand-detail-hero__art" aria-hidden="true">
        <span />
        <span />
      </div>
      <div className="customer-page__wide brand-detail-hero__inner">
        <div className="brand-detail-hero__logo">
          <img src={productImage(brand.logo, brand.name)} alt={`${brand.name} logo`} />
        </div>
        <div className="brand-detail-hero__copy">
          <p>Featured maison</p>
          <h1>{brand.name}</h1>
          {brand.description && <p className="brand-detail-hero__description">{brand.description}</p>}
          <a href={brand.collectionCount > 0 ? "#collections" : "#brand-products"}>
            {brand.collectionCount > 0 ? "Khám phá bộ sưu tập" : "Xem sản phẩm"} <ArrowDown size={15} />
          </a>
        </div>
      </div>
    </section>
  );
}
