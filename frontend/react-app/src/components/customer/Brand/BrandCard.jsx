import { Link } from "react-router-dom";

export default function BrandCard({ brand }) {
  if (!brand) return null;

  const { id, name, code, logo, description, status } = brand;

  return (
    <div
      className="
        group bg-zinc-900 border border-zinc-800 rounded-xl
        overflow-hidden flex flex-col
        transition-all duration-200
        hover:border-amber-400 hover:-translate-y-1 hover:shadow-lg
      "
    >
      {/* LOGO */}
      <div className="h-32 sm:h-36 md:h-40 bg-zinc-800 flex items-center justify-center p-4 sm:p-6">
        <img
          src={logo}
          alt={name}
          loading="lazy"
          className="
            max-h-full max-w-full object-contain
            transition-transform duration-300
            group-hover:scale-105
          "
        />
      </div>

      {/* CONTENT */}
      <div className="p-4 sm:p-5 flex flex-col flex-1">
        
        {/* NAME + STATUS */}
        <div className="flex items-start justify-between gap-2 mb-2">
          <h3 className="text-base sm:text-lg font-semibold line-clamp-1">
            {name}
          </h3>

          {/* optional status */}
          {status && (
            <span
              className={`
                text-[10px] px-2 py-1 rounded-full whitespace-nowrap
                ${
                  status === "active"
                    ? "bg-green-500/10 text-green-400"
                    : "bg-red-500/10 text-red-400"
                }
              `}
            >
              {status}
            </span>
          )}
        </div>

        {/* DESCRIPTION (optional) */}
        {description && (
          <p className="text-xs text-zinc-500 line-clamp-2 mb-3">
            {description}
          </p>
        )}

        {/* FOOTER */}
        <div className="mt-auto flex items-center justify-between">
          <span className="text-[11px] sm:text-xs text-zinc-500 uppercase">
            Code: {code}
          </span>

          <Link
            to={`/brands/${id}`}
            className="
              text-xs sm:text-sm font-medium text-amber-400
              hover:text-amber-300 transition
            "
          >
            Xem chi tiết →
          </Link>
        </div>
      </div>
    </div>
  );
}