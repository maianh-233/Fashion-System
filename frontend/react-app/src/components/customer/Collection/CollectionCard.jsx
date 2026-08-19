import { Link } from "react-router-dom";

export default function CollectionCard({ collection }) {
  if (!collection) return null;

  const { id, name, brand, season, year, cover_image } = collection;

  return (
    <article className="collection-card group overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900 shadow-sm transition duration-200 hover:-translate-y-1 hover:border-amber-400 hover:shadow-xl">
      <div className="aspect-[4/5] overflow-hidden bg-zinc-800">
        <img
          src={cover_image || "/placeholder-collection.jpg"}
          alt={name}
          loading="lazy"
          className="h-full w-full object-cover transition duration-300 group-hover:scale-105"
        />
      </div>

      <div className="collection-card__content flex min-h-44 flex-col bg-zinc-900 p-4">
        {/* BRAND */}
        {brand && (
            <p className="collection-card__brand text-[10px] font-semibold uppercase tracking-[0.28em] text-amber-300">
                {brand}
            </p>
        )}

        {/* NAME – ĐIỂM NHẤN CHÍNH */}
        <h2 className="collection-card__title mt-2 line-clamp-2 text-base font-semibold leading-snug text-zinc-100">
            {name}
        </h2>

        {/* SEASON */}
        <p className="collection-card__meta mt-2 text-xs text-zinc-400">
            {season} • {year}
        </p>

        {/* CTA – NỔI HƠN */}
        <Link
            to={`/collectiondetail?id=${id}`}
            className="
            mt-auto inline-flex min-h-11 w-full items-center justify-between gap-2
            px-3.5 py-2
            rounded-lg

            bg-amber-400 text-black
            text-[15px] font-semibold

            shadow-md shadow-amber-500/30

            hover:bg-amber-300
            hover:scale-[1.03]
            transition-all duration-200
            "
        >
             Xem chi tiết
            <span className="text-sm">→</span>
        </Link>
      </div>
    </article>
  );
}
