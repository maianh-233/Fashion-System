import { brand } from "../../hooks/brand";
import { collections } from "../../hooks/collections";

import BrandHero from "../../components/customer/Brand/BrandHero";
import BrandInfo from "../../components/customer/Brand//BrandInfo";
import CollectionSection from "../../components/customer/Brand/CollectionSection";

export default function BrandDetail() {
  return (
    <div className="min-h-screen w-full bg-[#0b0f14] text-gray-100">
      
      {/* HERO */}
      <BrandHero brand={brand} />

      {/* CONTENT */}
      <div className="max-w-8xl mx-auto px-6 py-12">
        <BrandInfo />
        <CollectionSection collections={collections} />
      </div>

    </div>
  );
}