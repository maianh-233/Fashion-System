import { useState, useRef, useEffect } from "react";
import {
  Package,
  Store,
  Layers,
  ChevronDown,
  Tag
} from "lucide-react";

const options = [
  {
    value: "product",
    label: "Tìm theo Sản phẩm",
    icon: Package,
  },
  {
    value: "brand",
    label: "Tìm theo Thương hiệu",
    icon: Store,
  },
  {
    value: "collection",
    label: "Tìm theo Bộ sưu tập",
    icon: Layers,
  },
    {
    value: "tg",
    label: "Tìm theo Tag",
    icon: Tag,
  },
];

export default function SearchTypeDropdown({ value, onChange }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  const current = options.find((o) => o.value === value);

  // click outside để đóng
  useEffect(() => {
    const handler = (e) => {
      if (!ref.current?.contains(e.target)) setOpen(false);
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  return (
    <div ref={ref} className="relative w-64">
      {/* BUTTON */}
      <button
        onClick={() => setOpen(!open)}
        className="w-full flex items-center justify-between gap-3 bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-3 text-sm hover:border-amber-400 transition"
      >
        <div className="flex items-center gap-3">
          <current.icon size={18} className="text-zinc-300" />
          <span>{current.label}</span>
        </div>
        <ChevronDown
          size={16}
          className={`transition ${open ? "rotate-180" : ""}`}
        />
      </button>

      {/* DROPDOWN */}
      {open && (
        <div className="absolute right-0 mt-3 w-full rounded-2xl border border-zinc-700 bg-zinc-900 shadow-xl overflow-hidden z-50">
          <ul className="p-2 space-y-1">
            {options.map((item) => {
              const Icon = item.icon;
              return (
                <li key={item.value}>
                  <button
                    onClick={() => {
                      onChange(item.value);
                      setOpen(false);
                    }}
                    className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm hover:bg-zinc-800 transition text-left"
                  >
                    <Icon size={18} className="text-zinc-400" />
                    <span>{item.label}</span>
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