import { ArrowRight, ChevronLeft, ChevronRight } from "lucide-react";
import { Link } from "react-router-dom";
import Button from "../common/Button";

/** Banner trang chủ; slide không có ảnh dùng nền chữ lớn thay vì ảnh minh họa. */
export default function HeroSlider({
  slides,
  currentIndex,
  onNext,
  onPrev,
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
            className={`home-hero__slide${slide.image ? "" : " home-hero__slide--plain"}`}
            aria-hidden={index !== currentIndex}
          >
            {slide.image
              ? <img src={slide.image} alt="" className="home-hero__image" />
              : <span className="home-hero__art" aria-hidden="true">{slide.title}</span>}
            <div className="home-hero__overlay" />
            <div className="home-hero__content">
              <p className="home-hero__eyebrow">{slide.eyebrow}</p>
              <h1>{slide.title}</h1>
              <p className="home-hero__description">{slide.description}</p>
              <Link to={slide.link} className="home-hero__cta" tabIndex={index === currentIndex ? undefined : -1}>
                {slide.cta} <ArrowRight size={16} aria-hidden="true" />
              </Link>
            </div>
          </article>
        ))}
      </div>

      {slides.length > 1 && (
        <div className="home-hero__nav">
          <Button type="button" variant="unstyled" onClick={onPrev} aria-label="Slide trước">
            <ChevronLeft size={19} />
          </Button>
          <span>{String(currentIndex + 1).padStart(2, "0")} / {String(slides.length).padStart(2, "0")}</span>
          <Button type="button" variant="unstyled" onClick={onNext} aria-label="Slide tiếp theo">
            <ChevronRight size={19} />
          </Button>
        </div>
      )}
    </section>
  );
}
