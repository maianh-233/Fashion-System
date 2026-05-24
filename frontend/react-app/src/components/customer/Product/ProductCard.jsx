function ProductCard({ product }) {
  const { name, tags, thumbnail, min_price } = product;

  return (
    <article className="group relative overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900 transition duration-500 hover:-translate-y-2 hover:border-amber-400">
      
      {/* TAG */}
      {tags?.length > 0 && (
        <div className="absolute left-4 top-4 z-10 flex flex-wrap gap-2">
          {tags.map(tag => (
            <span
              key={tag}
              className="rounded-full bg-amber-400 px-3 py-1 text-[10px] font-semibold tracking-wide text-black"
            >
              {tag}
            </span>
          ))}
        </div>
      )}

      {/* IMAGE */}
      <div className="relative h-56 overflow-hidden bg-zinc-950 sm:h-64 lg:h-72">
        <img
          src={thumbnail}
          alt={name}
          className="h-full w-full object-cover transition duration-700 group-hover:scale-110"
        />

        {/* OVERLAY */}
        <div className="absolute inset-0 bg-black/0 transition duration-500 group-hover:bg-black/30" />

        {/* CTA ON HOVER */}
        <span className="absolute bottom-6 left-1/2 -translate-x-1/2 translate-y-6 rounded-full border border-white/70 px-6 py-2 text-xs tracking-widest text-white opacity-0 transition-all duration-300 group-hover:translate-y-0 group-hover:opacity-100">
          XEM CHI TIẾT
        </span>
      </div>

      {/* INFO */}
      <div className="space-y-2 p-4 sm:p-5 lg:p-6">
        {/* NAME */}
        <h3 className="line-clamp-2 text-sm font-medium sm:text-base">
          {name}
        </h3>

        {/* PRICE */}
        {min_price && (
          <p className="text-sm font-semibold text-amber-400">
            Từ {min_price.toLocaleString("vi-VN")} VND
          </p>
        )}
      </div>
    </article>
  );
}

export default ProductCard;