const DEFAULT_OPTIONS = [
  { key: "women", label: "Thời trang nữ" },
  { key: "men", label: "Thời trang nam" },
  { key: "unisex", label: "Thiết kế unisex" },
];

/** Nhóm checkbox; {@code options} dạng {key, label, count?}. */
export default function CategoryFilter({ selected = [], onChange = () => {}, options = DEFAULT_OPTIONS, title = "Danh mục" }) {
  const toggle = (key) => {
    onChange(
      selected.includes(key)
        ? selected.filter((item) => item !== key)
        : [...selected, key],
    );
  };

  return (
    <div className="customer-filter__section mb-6">
      <h3>{title}</h3>

      <div className="space-y-3 text-sm">
        {options.map((item) => (
          <label key={item.key} className="customer-filter__option flex items-center gap-2">
            <input
              type="checkbox"
              checked={selected.includes(item.key)}
              onChange={() => toggle(item.key)}
            />
            <span className="flex-1">{item.label}</span>
            {item.count != null && <small className="text-zinc-500">{item.count}</small>}
          </label>
        ))}
      </div>
    </div>
  );
}
