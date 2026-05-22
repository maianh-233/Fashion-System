function BrandCard({ brand }) {
  return (
    <article className="rounded-3xl border border-zinc-800 bg-zinc-900 transition duration-300 hover:-translate-y-2 p-6 flex flex-col items-center justify-center text-center">
      <div className="text-6xl mb-4">{brand.icon}</div>
      <h3 className="text-lg font-medium">{brand.name}</h3>
      <p className="mt-2 text-sm text-zinc-400">{brand.category}</p>
    </article>
  );
}

export default function BrandsSection({ brands }) {
  return (
    <section className="mt-16 sm:mt-20 lg:mt-24">
      <div className="flex items-center justify-between mb-8 sm:mb-10">
        <h2 className="text-2xl font-light tracking-[0.18em] sm:text-3xl sm:tracking-[0.25em]">HÃNG HỢP TÁC</h2>
        <a
          href="/brands"
          className="px-6 py-2.5 text-sm font-medium rounded-2xl border border-amber-400 text-amber-400 transition hover:bg-amber-400 hover:text-black"
        >
          TIM HIEU THEM →
        </a>
      </div>
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 sm:gap-6 lg:grid-cols-4 lg:gap-8">
        {brands.slice(0, 4).map((brand) => (
          <BrandCard key={brand.id} brand={brand} />
        ))}
      </div>
    </section>
  );
}
