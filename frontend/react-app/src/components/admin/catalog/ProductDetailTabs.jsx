import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { variantApi } from "../../../hooks/catalogApi";
import Button from "../../common/Button";
import Pagination from "../../common/Pagination";
import ProductMetadataPanel from "./ProductMetadataPanel";

export default function ProductDetailTabs({ product }) {
  const navigate = useNavigate();
  const [tab, setTab] = useState("general");
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);
  const [variants, setVariants] = useState([]);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const loadVariants = useCallback(async (signal) => {
    setLoading(true);
    try {
      const result = await variantApi.listAll({ productId: product.id, keyword, page: page - 1,
        size: 10, sort: "sku,asc" }, { signal });
      setVariants(result?.content || []);
      setTotalPages(Math.max(1, result?.totalPages || 1));
      setError("");
    } catch (requestError) {
      if (requestError.name !== "AbortError") setError(requestError.message || "Không thể tải biến thể.");
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, [product.id, keyword, page]);

  useEffect(() => {
    if (tab !== "variants") return undefined;
    const controller = new AbortController();
    loadVariants(controller.signal);
    return () => controller.abort();
  }, [loadVariants, tab]);

  const tabs = [
    ["general", "Thông tin chung"], ["images", "Hình ảnh"],
    ["attributes", "Thuộc tính & Tag"], ["variants", "Biến thể"],
  ];
  return <section className="mt-6 border-t border-zinc-800 pt-5">
    <div className="mb-4 flex flex-wrap gap-2" role="tablist">{tabs.map(([key, label]) => <Button key={key} type="button" role="tab" aria-selected={tab === key} onClick={() => setTab(key)}>{label}</Button>)}</div>
    {tab === "general" && <div className="space-y-2 text-sm text-zinc-300"><p>{product.code} · {product.name}</p><p>Slug: {product.slug || "—"}</p><p>{product.description || "Chưa có mô tả."}</p></div>}
    {tab === "images" && <div>{product.imageUrl ? <img src={product.imageUrl} alt={product.name} className="max-h-72 rounded-xl object-contain" /> : <p className="text-sm text-zinc-400">Chưa có ảnh sản phẩm.</p>}</div>}
    {tab === "attributes" && <ProductMetadataPanel productId={product.id} />}
    {tab === "variants" && <div className="space-y-3"><input value={keyword} onChange={(event) => { setKeyword(event.target.value); setPage(1); }} placeholder="Tìm SKU hoặc barcode…" className="h-10 w-full rounded-xl border border-zinc-700 bg-zinc-800 px-3" />{error && <p className="text-sm text-red-300">{error}</p>}{loading ? <p className="text-sm text-zinc-400">Đang tải biến thể…</p> : variants.length === 0 ? <p className="text-sm text-zinc-400">Chưa có biến thể.</p> : <div className="space-y-2">{variants.map((variant) => <div key={variant.id} className="flex items-center justify-between rounded-xl bg-zinc-800 p-3 text-sm"><span>{variant.sku} · {variant.color || "—"} / {variant.size || "—"}</span><span>{Number(variant.salePrice ?? variant.price).toLocaleString("vi-VN")} ₫</span></div>)}</div>}<Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} /><Button permission="PRODUCT_VARIANT_VIEW" type="button" onClick={() => navigate(`/admin/product-variants?productId=${product.id}`)}>Quản lý biến thể</Button></div>}
  </section>;
}
