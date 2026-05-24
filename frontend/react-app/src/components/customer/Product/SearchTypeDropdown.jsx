import { useState, useRef, useEffect } from "react";
import {
  Package,
  Store,
  Layers,
  ChevronDown,
  Tag,
} from "lucide-react";

const options = [
  { value: "product", label: "Sản phẩm", icon: Package },
  { value: "brand", label: "Thương hiệu", icon: Store },
  { value: "collection", label: "Bộ sưu tập", icon: Layers },
  { value: "tag", label: "Tag", icon: Tag },
];

export default function SearchTypeDropdown({ value, onChange }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  const current =
    options.find((o) => o.value === value) || options[0];

  // click outside
  useEffect(() => {
    const handler = (e) => {
      if (!ref.current?.contains(e.target)) setOpen(false);
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  return (
    <div ref={ref} className="relative w-full lg:w-64">
      
      {/* BUTTON */}
      <button
        onClick={() => setOpen(!open)}
        className="
          w-full
          flex items-center justify-between
          gap-3
          bg-zinc-800 border border-zinc-700
          rounded-xl
          px-4 py-3
          text-sm
          hover:border-amber-400
          transition
        "
      >
        <div className="flex items-center gap-3 min-w-0">
          <current.icon size={18} className="text-zinc-300 shrink-0" />
          <span className="truncate">{current.label}</span>
        </div>

        <ChevronDown
          size={16}
          className={`transition shrink-0 ${
            open ? "rotate-180" : ""
          }`}
        />
      </button>

      {/* DROPDOWN */}
      {open && (
        <div
          className="
            absolute z-50 mt-2
            w-full
            lg:min-w-[260px]
            bg-zinc-900 border border-zinc-700
            rounded-xl shadow-xl
            overflow-hidden
          "
        >
          <ul className="p-2">
            {options.map((item) => {
              const Icon = item.icon;

              return (
                <li key={item.value}>
                  <button
                    onClick={() => {
                      onChange(item.value);
                      setOpen(false);
                    }}
                    className="
                      w-full
                      flex items-center gap-3
                      px-4 py-3
                      rounded-lg
                      text-sm
                      hover:bg-zinc-800
                      active:bg-zinc-700
                      transition
                      text-left
                    "
                  >
                    <Icon size={18} className="text-zinc-400" />
                    <span className="truncate">{item.label}</span>
                  </button>
                </li>
              );
            })}
          </ul>
        </div>
      )}
    </div>
  );
}