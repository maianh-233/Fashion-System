import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { productApi } from "../../../hooks/catalogApi";
import { useAdminPermissions } from "../../../contexts/AdminPermissionsContext";
import Button from "../../common/Button";
import Pagination from "../../common/Pagination";

export default function CollectionProductsPanel({ collection }) {
  const navigate = useNavigate();
  const { isGlobal } = useAdminPermissions();
  const [tab, setTab] = useState("info");
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);
  const [products, setProducts] = useState([]);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [selected, setSelected] = useState(null);

  const load = useCallback(async (signal) => {
    setLoading(true);
    try {
      const result = await productApi.list({ collectionId: collection.id, keyword, page: page - 1,
        size: 10, sort: "name,asc", status: isGlobal ? "ALL" : undefined }, { signal });
      setProducts(result?.content || []);
      setTotalPages(Math.max(1, result?.totalPages || 1));
      setError("");
    } catch (requestError) {
      if (requestError.name !== "AbortError") setError(requestError.message || "Không thể tải sản phẩm.");
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, [collection.id, isGlobal, keyword, page]);

  useEffect(() => {
    if (tab !== "products") return undefined;
    const controller = new AbortController();
    load(controller.signal);
    return () => controller.abort();
  }, [load, tab]);

  return <section className="mt-6 border-t border-zinc-800 pt-5">
    <div className="mb-4 flex gap-2" role="tablist">
      <Button type="button" role="tab" aria-selected={tab === "info"} onClick={() => setTab("info")}>Thông tin bộ sưu tập</Button>
      <Button type="button" role="tab" aria-selected={tab === "products"} onClick={() => setTab("products")}>Sản phẩm</Button>
    </div>
    {tab === "info" ? <div className="space-y-2 text-sm text-zinc-300">
      <p>{collection.description || "Chưa có mô tả."}</p>
      <p>Mùa: {collection.season || "—"} · Năm: {collection.year || "—"} · Ngày ra mắt: {collection.releaseDate || "—"}</p>
    </div> : <div className="space-y-3">
      <input value={keyword} onChange={(event) => { setKeyword(event.target.value); setPage(1); }} placeholder="Tìm tên, mã hoặc slug sản phẩm…" className="h-10 w-full rounded-xl border border-zinc-700 bg-zinc-800 px-3" />
      {error && <p className="text-sm text-red-300">{error}</p>}
      {loading ? <p className="text-sm text-zinc-400">Đang tải sản phẩm…</p> : products.length === 0 ? <p className="text-sm text-zinc-400">Chưa có sản phẩm.</p> : <div className="overflow-x-auto"><table className="w-full text-sm"><thead><tr><th className="p-2 text-left">Mã</th><th className="p-2 text-left">Sản phẩm</th><th className="p-2 text-left">Trạng thái</th><th className="p-2 text-left">Thao tác</th></tr></thead><tbody>{products.map((product) => <tr key={product.id} className="border-t border-zinc-800"><td className="p-2">{product.code || "—"}</td><td className="p-2">{product.name}</td><td className="p-2">{product.status}</td><td className="p-2"><Button type="button" onClick={() => setSelected(product)}>Xem</Button> <Button permission="PRODUCT_VARIANT_VIEW" type="button" onClick={() => navigate(`/admin/product-variants?productId=${product.id}`)}>Xem biến thể</Button></td></tr>)}</tbody></table></div>}
      <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
      {selected && <div className="rounded-xl border border-zinc-700 bg-zinc-800 p-3 text-sm"><div className="flex justify-between"><strong>{selected.code} · {selected.name}</strong><Button type="button" onClick={() => setSelected(null)}>Đóng</Button></div><p className="mt-2">{selected.description || "Chưa có mô tả."}</p><p className="mt-1">Slug: {selected.slug}</p></div>}
    </div>}
  </section>;
}
