import { useState } from "react";
import { ChevronDown } from "lucide-react";

/**
 * Lọc theo cây danh mục từ API: chọn danh mục cha là lấy cả danh mục con (backend tự mở rộng), nên khi đã chọn
 * cha thì các ô con bị khóa.
 */
export default function CategoryTreeFilter({ categories = [], selected = [], onChange = () => {} }) {
  const [expanded, setExpanded] = useState(() => new Set(
    categories.filter((c) => c.children?.some((child) => selected.includes(child.id))).map((c) => c.id)));

  const toggle = (id, childIds = []) => {
    if (selected.includes(id)) onChange(selected.filter((item) => item !== id));
    else onChange([...selected.filter((item) => !childIds.includes(item)), id]);
  };
  const toggleOpen = (id) => setExpanded((current) => {
    const next = new Set(current);
    if (next.has(id)) next.delete(id); else next.add(id);
    return next;
  });

  if (!categories.length) return null;
  return (
    <div className="customer-filter__section mb-6">
      <h3>Loại sản phẩm</h3>
      <ul className="space-y-2 text-sm">
        {categories.map((category) => {
          const childIds = (category.children || []).map((child) => child.id);
          const parentChecked = selected.includes(category.id);
          const open = expanded.has(category.id);
          return (
            <li key={category.id}>
              <div className="flex items-center gap-2">
                <label className="customer-filter__option flex flex-1 items-center gap-2">
                  <input type="checkbox" checked={parentChecked} onChange={() => toggle(category.id, childIds)} />
                  <span className="flex-1">{category.name}</span>
                  <small className="text-zinc-500">{category.productCount}</small>
                </label>
                {childIds.length > 0 && (
                  <button type="button" className="customer-filter__expand" onClick={() => toggleOpen(category.id)}
                    aria-expanded={open} aria-label={`${open ? "Thu gọn" : "Mở"} ${category.name}`}>
                    <ChevronDown size={14} className={`transition ${open ? "rotate-180" : ""}`} />
                  </button>
                )}
              </div>
              {open && (
                <ul className="mt-2 space-y-2 pl-6">
                  {category.children.map((child) => (
                    <li key={child.id}>
                      <label className="customer-filter__option flex items-center gap-2">
                        <input type="checkbox" disabled={parentChecked}
                          checked={parentChecked || selected.includes(child.id)}
                          onChange={() => toggle(child.id)} />
                        <span className="flex-1">{child.name}</span>
                        <small className="text-zinc-500">{child.productCount}</small>
                      </label>
                    </li>
                  ))}
                </ul>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
