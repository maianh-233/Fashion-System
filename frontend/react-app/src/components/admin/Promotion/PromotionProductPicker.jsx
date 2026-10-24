import { useEffect, useState } from "react";
import { Search, X } from "lucide-react";
import { AdminFormSection } from "../common/AdminForm";
import { productApi } from "../../../api/admin/catalogApi";

export default function PromotionProductPicker({ mode, selected, onChange }) {
  const isView = mode === "view";
  const [keyword, setKeyword] = useState("");
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (isView || !keyword.trim()) return undefined;
    const controller = new AbortController();
    const timer = setTimeout(() => {
      setLoading(true);
      setError("");
      productApi.list({ keyword: keyword.trim(), size: 10 }, { signal: controller.signal })
        .then((page) => setResults(page?.content || []))
        .catch((requestError) => {
          if (!controller.signal.aborted) setError(requestError.message || "Không tải được sản phẩm");
        })
        .finally(() => setLoading(false));
    }, 300);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [keyword, isView]);

  const add = (product) => {
    if (!selected.some((item) => item.id === product.id)) {
      onChange([...selected, { id: product.id, code: product.code, name: product.name }]);
    }
  };

  return (
    <AdminFormSection title="Sản phẩm áp dụng" description="Khuyến mãi chỉ áp dụng cho các sản phẩm được chọn.">
      {!isView && (
        <div className="relative mb-3">
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="Tìm sản phẩm theo tên hoặc mã..."
            className="admin-field__control w-full pl-10"
          />
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
          {keyword.trim() && (
            <div className="mt-2 max-h-48 overflow-y-auto rounded-xl border border-zinc-700 bg-zinc-900">
              {loading && <p className="px-3 py-2 text-sm text-zinc-400">Đang tìm...</p>}
              {error && <p className="px-3 py-2 text-sm text-red-400">{error}</p>}
              {!loading && !error && results.length === 0 && (
                <p className="px-3 py-2 text-sm text-zinc-400">Không tìm thấy sản phẩm.</p>
              )}
              {!loading && results.map((product) => {
                const picked = selected.some((item) => item.id === product.id);
                return (
                  <button
                    key={product.id}
                    type="button"
                    disabled={picked}
                    onClick={() => add(product)}
                    className="flex w-full justify-between px-3 py-2 text-left text-sm hover:bg-zinc-800 disabled:opacity-50"
                  >
                    <span>{product.name}</span>
                    <span className="text-zinc-500">{picked ? "Đã chọn" : product.code}</span>
                  </button>
                );
              })}
            </div>
          )}
        </div>
      )}
      {selected.length === 0 ? (
        <p className="text-sm text-zinc-400">Chưa chọn sản phẩm nào.</p>
      ) : (
        <div className="flex flex-wrap gap-2">
          {selected.map((product) => (
            <span key={product.id} className="flex items-center gap-2 rounded-full bg-zinc-800 px-3 py-1 text-sm">
              {product.name}
              {product.code && <span className="text-zinc-500">{product.code}</span>}
              {!isView && (
                <button
                  type="button"
                  aria-label={`Bỏ ${product.name}`}
                  onClick={() => onChange(selected.filter((item) => item.id !== product.id))}
                  className="text-zinc-400 hover:text-red-400"
                >
                  <X size={14} />
                </button>
              )}
            </span>
          ))}
        </div>
      )}
    </AdminFormSection>
  );
}
