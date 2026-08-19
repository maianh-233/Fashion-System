export default function BrandHero({ brand }) {
  return (
    <div className="min-h-[430px] py-10 flex items-center text-white relative hero-bg sm:h-[520px] sm:py-0">
      <div className="w-full max-w-7xl mx-auto px-4 sm:px-6 z-10">
        <img
          src={brand.logo}
          alt={brand.name}
          className="w-24 h-24 bg-white rounded-2xl p-3 mb-5 shadow-2xl sm:w-36 sm:h-36 sm:rounded-3xl sm:p-5 sm:mb-6"
        />

        <h1 className="text-4xl font-bold mb-4 break-words sm:text-6xl">{brand.name}</h1>

        <p className="text-base leading-relaxed text-gray-300 mb-7 sm:text-xl sm:mb-8">
          {brand.description}
        </p>

        <a
          href="#collections"
          className="inline-flex min-h-12 w-full items-center justify-center gap-3 bg-white text-black px-5 py-3 rounded-2xl font-semibold sm:w-auto sm:px-8 sm:py-4"
        >
          Khám phá bộ sưu tập ↓
        </a>
      </div>
    </div>
  );
}
