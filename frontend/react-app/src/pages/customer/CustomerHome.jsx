import { useMemo, useState } from "react";
import BestSellerSection from "../../features/storefront/components/BestSellerSection";
import BrandsSection from "../../features/storefront/components/BrandsSection";
import CategoryGrid from "../../features/storefront/components/CategoryGrid";
import ExpertSection from "../../features/storefront/components/ExpertSection";
import HeroSlider from "../../features/storefront/components/HeroSlider";
import StorefrontFooter from "../../features/storefront/components/StorefrontFooter";
import StorefrontHeader from "../../features/storefront/components/StorefrontHeader";
import ValuesSection from "../../features/storefront/components/ValuesSection";
import VideoSection from "../../features/storefront/components/VideoSection";
import {
    bestSellerProducts,
    brands,
    categories,
    experts,
    heroSlides,
    navLinks,
    values,
} from "../../features/storefront/data/storefrontData";
import { useHeroSlider } from "../../features/storefront/hooks/useHeroSlider";

export default function CustomerHome() {
  const [activeCollectionId, setActiveCollectionId] = useState(heroSlides[0]?.id ?? null);
  const { currentIndex, next, prev } = useHeroSlider(heroSlides.length);

  const activeCollection = useMemo(
    () => heroSlides.find((slide) => slide.id === activeCollectionId),
    [activeCollectionId]
  );

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-200">
      <StorefrontHeader navLinks={navLinks} cartCount={3} />
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

      <StorefrontFooter />
    </div>
  );
}
