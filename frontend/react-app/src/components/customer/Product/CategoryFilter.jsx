const categories = [
  { key: "women", label: "Thời trang nữ" },
  { key: "men", label: "Thời trang nam" },
  { key: "unisex", label: "Thiết kế unisex" },
];

export default function CategoryFilter({ selected = [], onChange = () => {} }) {
  const toggle = (key) => {
    onChange(
      selected.includes(key)
        ? selected.filter((item) => item !== key)
        : [...selected, key],
    );
  };

  return (
    <div className="customer-filter__section mb-6">
      <h3>Danh mục</h3>

      <div className="space-y-3 text-sm">
        {categories.map((item) => (
          <label key={item.key} className="customer-filter__option flex items-center gap-2">
            <input
              type="checkbox"
              checked={selected.includes(item.key)}
              onChange={() => toggle(item.key)}
            />
            {item.label}
          </label>
        ))}
      </div>
    </div>
  );
}
