import { Link } from "react-router-dom";

export default function CollectionCard({ collection }) {
  if (!collection) return null;

  const { id, name, brand, season, year, cover_image } = collection;

  return (
    <div className="group bg-zinc-900 border border-zinc-800 rounded-xl overflow-hidden hover:border-amber-400 transition">
      {/* IMAGE – thấp hơn */}
      <div className="h-48 sm:h-80 bg-zinc-800 overflow-hidden">
        <img
          src={cover_image || "/placeholder-collection.jpg"}
          alt={name}
          className="w-full h-full object-cover group-hover:scale-105 transition duration-300"
        />
      </div>

      {/* CONTENT – nổi hơn nhưng vẫn gọn */}
      <div
        className="
        p-3 space-y-2
        bg-gradient-to-b from-zinc-900 via-zinc-900 to-zinc-950
        "
      >
        {/* BRAND */}
        {brand && (
            <p className="text-[10px] uppercase tracking-[0.28em] text-amber-300 font-semibold">
                {brand}
            </p>
        )}

        {/* NAME – ĐIỂM NHẤN CHÍNH */}
        <h3 className="text-[15px] leading-tightfont-semiboldtext-whiteline-clamp-2">
            {name}
        </h3>

        {/* SEASON */}
        <p className="text-[12px]text-zinc-400">
            {season} • {year}
        </p>

        {/* CTA – NỔI HƠN */}
        <Link
            to={`/collections/${id}`}
            className="
            inline-flex items-center gap-2
            px-3.5 py-1.5
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
    </div>
  );
}
