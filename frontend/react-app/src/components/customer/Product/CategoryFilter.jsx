import { useState } from "react";

export default function CategoryFilter() {
  const [selected, setSelected] = useState({
    clothing: false,
    men: false,
    women: false,
    unisex: false,
  });

  const toggle = (key) => {
    setSelected((prev) => ({
      ...prev,
      [key]: !prev[key],
    }));
  };

  return (
    <div className="mb-8">
      <h3 className="text-amber-400 font-medium mb-3">Danh mục</h3>

      <div className="space-y-2 text-sm">
        {/* Parent */}
        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            checked={selected.clothing}
            onChange={() => toggle("clothing")}
            className="accent-amber-400"
          />
          Quần áo
        </label>

        {/* Children */}
        <div className="pl-6 space-y-2 border-l border-zinc-700 ml-2">
          {[
            { key: "men", label: "Nam" },
            { key: "women", label: "Nữ" },
            { key: "unisex", label: "Unisex" },
          ].map((item) => (
            <label key={item.key} className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={selected[item.key]}
                onChange={() => toggle(item.key)}
                className="accent-amber-400"
              />
              {item.label}
            </label>
          ))}
        </div>
      </div>
    </div>
  );
}