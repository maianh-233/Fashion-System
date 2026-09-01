import { ArrowRight, ChevronLeft, ChevronRight } from "lucide-react";
import Button from "../common/Button";

export default function HeroSlider({
  slides,
  currentIndex,
  onNext,
  onPrev,
  onViewCollection,
}) {
  return (
    <section className="home-hero" aria-roledescription="carousel" aria-label="Bộ sưu tập nổi bật">
      <div
        className="home-hero__track"
        style={{ transform: `translateX(-${currentIndex * 100}%)` }}
      >
        {slides.map((slide, index) => (
          <article
            key={slide.id}
            className="home-hero__slide"
            aria-hidden={index !== currentIndex}
          >
            <img src={slide.image} alt="" className="home-hero__image" />
            <div className="home-hero__overlay" />
            <div className="home-hero__content">
              <p className="home-hero__eyebrow">{slide.season}</p>
              <h1>{slide.title}</h1>
              <p className="home-hero__description">{slide.description}</p>
              <Button
                type="button"
                variant="unstyled"
                className="home-hero__cta"
                onClick={() => onViewCollection(slide.id)}
              >
                Xem bộ sưu tập <ArrowRight size={16} aria-hidden="true" />
              </Button>
            </div>
          </article>
        ))}
      </div>

      <div className="home-hero__nav">
        <Button type="button" variant="unstyled" onClick={onPrev} aria-label="Slide trước">
          <ChevronLeft size={19} />
        </Button>
        <span>{String(currentIndex + 1).padStart(2, "0")} / {String(slides.length).padStart(2, "0")}</span>
        <Button type="button" variant="unstyled" onClick={onNext} aria-label="Slide tiếp theo">
          <ChevronRight size={19} />
        </Button>
      </div>
    </section>
  );
}
