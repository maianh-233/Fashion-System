export default function BrandHero({ brand }) {
  return (
    <div className="h-[520px] flex items-center text-white relative hero-bg">
      <div className="max-w-7xl mx-auto px-6 z-10">
        <img
          src={brand.logo}
          alt={brand.name}
          className="w-36 h-36 bg-white rounded-3xl p-5 mb-6 shadow-2xl"
        />

        <h1 className="text-6xl font-bold mb-4">{brand.name}</h1>

        <p className="text-xl text-gray-300 mb-8">
          {brand.description}
        </p>

        <a
          href="#collections"
          className="inline-flex items-center gap-3 bg-white text-black px-8 py-4 rounded-2xl font-semibold"
        >
          Khám phá bộ sưu tập ↓
        </a>
      </div>
    </div>
  );
}