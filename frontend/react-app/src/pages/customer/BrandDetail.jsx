import { brand } from "../../mock/brand";
import { collections } from "../../mock/collections";

import BrandHero from "../../components/customer/Brand/BrandHero";
import BrandInfo from "../../components/customer/Brand//BrandInfo";
import CollectionSection from "../../components/customer/Brand/CollectionSection";

export default function BrandDetail() {
  return (
    <div className="customer-page customer-detail-page min-h-screen w-full text-gray-100">
      
      {/* HERO */}
      <BrandHero brand={brand} />

      {/* CONTENT */}
      <div className="customer-page__wide px-4 py-8 sm:px-6 sm:py-10">
        <BrandInfo />
        <CollectionSection collections={collections} />
      </div>

    </div>
  );
}
