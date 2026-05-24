function BrandCard({ brand }) {
  return (
    <article
      className="
        relative
        min-h-[150px] sm:min-h-[180px]
        rounded-2xl sm:rounded-3xl
        border border-zinc-800
        bg-zinc-900
        p-4 sm:p-6
        flex flex-col items-center justify-center text-center
        cursor-pointer
        transition-all duration-300 ease-out
        active:scale-[0.97]
        sm:hover:-translate-y-2
        sm:hover:border-amber-400
        sm:hover:shadow-[0_20px_40px_-20px_rgba(251,191,36,0.35)]
      "
    >
      {/* Icon */}
      <div className="mb-3 text-4xl sm:text-5xl lg:text-6xl transition-transform duration-300 sm:group-hover:scale-110">
        {brand.icon}
      </div>

      {/* Name */}
      <h3 className="text-sm sm:text-base font-medium tracking-wide">
        {brand.name}
      </h3>

      {/* Category */}
      <p className="mt-1 text-xs sm:text-sm text-zinc-400">
        {brand.category}
      </p>
    </article>
  );
}

export default function BrandsSection({ brands }) {
  return (
    <section className="mt-12 sm:mt-20 lg:mt-24 px-4 sm:px-0">
      
      {/* Header */}
      <div className="flex items-center justify-between mb-6 sm:mb-10">
        <h2 className="text-lg sm:text-2xl font-light tracking-[0.18em] sm:tracking-[0.25em]">
          HÃNG HỢP TÁC
        </h2>

        <a
          href="/brands"
          className="
            px-4 sm:px-6
            py-2
            text-xs sm:text-sm
            font-medium
            rounded-xl sm:rounded-2xl
            border border-amber-400
            text-amber-400
            transition-all duration-300
            hover:bg-amber-400 hover:text-black
            active:scale-95
          "
        >
          TÌM HIỂU →
        </a>
      </div>

      {/* Grid */}
      <div
        className="
          grid grid-cols-2 gap-4
          sm:grid-cols-2 sm:gap-6
          lg:grid-cols-4 lg:gap-8
        "
      >
        {brands.slice(0, 4).map((brand) => (
          <BrandCard key={brand.id} brand={brand} />
        ))}
      </div>
    </section>
  );
}