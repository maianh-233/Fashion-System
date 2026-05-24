import { Link } from "react-router-dom";

export default function BrandCard({ brand }) {
  if (!brand) return null;

  const {
    id,
    name,
    code,
    logo,
    description,
    status,
  } = brand;

  return (
    <div className="group bg-zinc-900 border border-zinc-800 rounded-xl overflow-hidden hover:border-amber-400 transition flex flex-col">
      {/* LOGO */}
      <div className="h-40 bg-zinc-800 flex items-center justify-center p-6">
        <img
          src={logo}
          alt={name}
          className="max-h-full max-w-full object-contain"
        />
      </div>

      {/* CONTENT */}
      <div className="p-5 flex flex-col flex-1">
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-lg font-semibold line-clamp-1">{name}</h3>
        </div>


        {/* FOOTER */}
        <div className="mt-auto flex items-center justify-between">
          <span className="text-xs text-zinc-500 uppercase">
            Code: {code}
          </span>

          <Link
            to={`/brands/${id}`}
            className="text-sm font-medium text-amber-400 hover:text-amber-300"
          >
            Xem chi tiết →
          </Link>
        </div>
      </div>
    </div>
  );
}