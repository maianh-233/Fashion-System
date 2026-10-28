import { useEffect, useState } from "react";
import BestSellerSection from "../../components/customer/BestSellerSection";
import BrandsSection from "../../components/customer/BrandsSection";
import CategoryGrid from "../../components/customer/CategoryGrid";
import ExpertSection from "../../components/customer/ExpertSection";
import HeroSlider from "../../components/customer/HeroSlider";
import ValuesSection from "../../components/customer/ValuesSection";
import VideoSection from "../../components/customer/VideoSection";
import { experts, values } from "../../config/storefrontContent";
import { useHeroSlider } from "../../hooks/useHeroSlider";
import { storefrontApi } from "../../api/storefront/storefrontApi";
import { featuredCategories, heroSlidesFrom } from "./homeLogic";

export default function CustomerHome() {
  // null khi đang tải; lỗi API vẫn hiện slide mặc định thay vì để trống banner.
  const [slides, setSlides] = useState(null);
  const [categories, setCategories] = useState([]);
  const { currentIndex, next, prev } = useHeroSlider(slides?.length || 1);

  useEffect(() => {
    const controller = new AbortController();
    storefrontApi.collections("page=0&size=8", { signal: controller.signal })
      .then(page => setSlides(heroSlidesFrom(page?.content)))
      .catch(() => { if (!controller.signal.aborted) setSlides(heroSlidesFrom([])); });
    storefrontApi.filters({ signal: controller.signal })
      .then(filters => setCategories(featuredCategories(filters)))
      .catch(() => {});
    return () => controller.abort();
  }, []);

  return (
    <div className="customer-home">
      {slides
        ? <HeroSlider slides={slides} currentIndex={currentIndex} onNext={next} onPrev={prev} />
        : <section className="home-hero" aria-busy="true" aria-label="Đang tải bộ sưu tập nổi bật" />}

      <main className="customer-home__content">
        <CategoryGrid categories={categories} />
        <BestSellerSection />
        <BrandsSection />
        <VideoSection />
        <ValuesSection values={values} />
        <ExpertSection experts={experts} />
      </main>
    </div>
  );
}
