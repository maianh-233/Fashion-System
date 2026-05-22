import { ChevronLeft, ChevronRight } from "lucide-react";

export default function HeroSlider({ slides, currentIndex, onNext, onPrev, onViewCollection }) {
  return (
    <section className="relative h-[70vh] min-h-[300px] overflow-hidden sm:h-[78vh] lg:h-screen">
      <div
        className="flex h-full transition-transform duration-700 ease-in-out"
        style={{ transform: `translateX(-${currentIndex * 100}%)` }}
      >
        {slides.map((slide) => (
          <article
            key={slide.id}
            className="relative h-full w-full flex-shrink-0 bg-cover bg-center"
            style={{ backgroundImage: `url('${slide.image}')` }}
          >
            <div className="absolute inset-0 bg-gradient-to-b from-black/40 via-black/60 to-black/80" />
            <div className="absolute bottom-16 left-4 max-w-[90%] text-white sm:bottom-20 sm:left-8 sm:max-w-2xl md:left-16 lg:bottom-24 lg:left-20">
              <p className="mb-2 text-xs uppercase tracking-[0.3em] text-amber-300 sm:text-sm sm:tracking-[0.35em]">{slide.season}</p>
              <h1 className="font-serif text-4xl font-light tracking-wider sm:text-5xl md:text-6xl lg:text-7xl">{slide.title}</h1>
              <p className="mt-3 text-sm text-zinc-200 sm:mt-4 sm:text-base md:text-lg">{slide.description}</p>
              <button
                type="button"
                onClick={() => onViewCollection(slide.id)}
                className="mt-6 rounded-full border border-white px-6 py-3 text-xs tracking-[0.15em] transition hover:bg-white hover:text-black sm:mt-8 sm:px-8 sm:py-3 sm:text-sm sm:tracking-[0.2em] lg:px-10 lg:py-4"
              >
                XEM BỘ SƯU TẬP
              </button>
            </div>
          </article>
        ))}
      </div>

      <button
        type="button"
        onClick={onPrev}
        className="absolute left-2 top-1/2 -translate-y-1/2 rounded-full bg-black/20 p-1.5 text-white/70 transition hover:text-white sm:left-4 sm:p-2"
        aria-label="Previous slide"
      >
        <ChevronLeft size={30} />
      </button>
      <button
        type="button"
        onClick={onNext}
        className="absolute right-2 top-1/2 -translate-y-1/2 rounded-full bg-black/20 p-1.5 text-white/70 transition hover:text-white sm:right-4 sm:p-2"
        aria-label="Next slide"
      >
        <ChevronRight size={30} />
      </button>
    </section>
  );
}
