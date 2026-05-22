import { useMemo, useState } from "react";
import BestSellerSection from "../../components/customer/BestSellerSection";
import BrandsSection from "../../components/customer/BrandsSection";
import CategoryGrid from "../../components/customer/CategoryGrid";
import ExpertSection from "../../components/customer/ExpertSection";
import HeroSlider from "../../components/customer/HeroSlider";
import ValuesSection from "../../components/customer/ValuesSection";
import VideoSection from "../../components/customer/VideoSection";
import {
    bestSellerProducts,
    brands,
    categories,
    experts,
    heroSlides,
    values,
} from "../../hooks/storefrontData";
import { useHeroSlider } from "../../hooks/useHeroSlider";

export default function CustomerHome() {
  const [activeCollectionId, setActiveCollectionId] = useState(heroSlides[0]?.id ?? null);
  const { currentIndex, next, prev } = useHeroSlider(heroSlides.length);

  const activeCollection = useMemo(
    () => heroSlides.find((slide) => slide.id === activeCollectionId),
    [activeCollectionId]
  );

  return (
    <>
      <HeroSlider
        slides={heroSlides}
        currentIndex={currentIndex}
        onNext={next}
        onPrev={prev}
        onViewCollection={setActiveCollectionId}
      />

      <main className="w-full px-4 py-10 sm:px-6 sm:py-14 lg:px-10 lg:py-16 2xl:px-16">
        <CategoryGrid categories={categories} />
        <BestSellerSection products={bestSellerProducts} />
        <BrandsSection brands={brands} />
        <VideoSection />
        <ValuesSection values={values} />
        <ExpertSection experts={experts} />
      </main>
    </>
  );
}
