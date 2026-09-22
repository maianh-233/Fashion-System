import { useNavigate } from "react-router-dom";
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
} from "../../mock/storefrontData";
import { useHeroSlider } from "../../hooks/useHeroSlider";

export default function CustomerHome() {
  const navigate = useNavigate();
  const { currentIndex, next, prev } = useHeroSlider(heroSlides.length);

  return (
    <div className="customer-home">
      <HeroSlider
        slides={heroSlides}
        currentIndex={currentIndex}
        onNext={next}
        onPrev={prev}
        onViewCollection={(id) => navigate(`/collectiondetail?id=${id}`)}
      />

      <main className="customer-home__content">
        <CategoryGrid categories={categories} />
        <BestSellerSection products={bestSellerProducts} />
        <BrandsSection brands={brands} />
        <VideoSection />
        <ValuesSection values={values} />
        <ExpertSection experts={experts} />
      </main>
    </div>
  );
}
