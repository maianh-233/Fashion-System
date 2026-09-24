import { useCallback, useEffect, useState } from "react";
import { Eye, Link } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { productApi } from "../../../api/catalogApi";
import { useAdminPermissions } from "../../../contexts/AdminPermissionsContext";
import Button from "../../common/Button";
import Pagination from "../../common/Pagination";
import ProductDialog from "../common/ProductDialog";
import { createProductViewDialog } from "../common/productDialogLogic";
import ProductDetailTabs from "./ProductDetailTabs";
import {
  productTableColumns,
  productVariantAction,
} from "./productTablePresentation";

export default function CollectionProductsPanel({ collection }) {
  const navigate = useNavigate();
  const { isGlobal } = useAdminPermissions();
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);
  const [products, setProducts] = useState([]);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [productDialog, setProductDialog] = useState(null);

  const load = useCallback(async (signal) => {
    setLoading(true);
    try {
      const result = await productApi.list({ collectionId: collection.id, keyword, page: page - 1,
        size: 10, sort: "name,asc", status: isGlobal ? "ALL" : undefined }, { signal });
      setProducts(result?.content || []);
      setTotalPages(Math.max(1, result?.page?.totalPages ?? result?.totalPages ?? 1));
      setError("");
    } catch (requestError) {
      if (requestError.name !== "AbortError") setError(requestError.message || "Không thể tải sản phẩm.");
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, [collection.id, isGlobal, keyword, page]);

  useEffect(() => {
    const controller = new AbortController();
    Promise.resolve().then(() => {
      if (!controller.signal.aborted) load(controller.signal);
    });
    return () => controller.abort();
  }, [load]);

  return <section className="space-y-4">
      <div className="admin-catalog-toolbar rounded-2xl border border-zinc-800 bg-zinc-900 p-4">
        <input value={keyword} onChange={(event) => { setKeyword(event.target.value); setPage(1); }} placeholder="Tìm tên, mã hoặc slug sản phẩm…" className="h-10 w-full rounded-xl border border-zinc-700 bg-zinc-800 px-3" />
      </div>
      {error && <p className="text-sm text-red-300">{error}</p>}
      <div className="admin-catalog-table overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900">
        <div className="overflow-x-auto"><table className="w-full min-w-[1400px] text-sm">
          <thead className="bg-zinc-800/70"><tr>{productTableColumns.map((column) => <th key={column.key} className="px-4 py-3 text-left" style={column.minWidth ? { minWidth: column.minWidth } : undefined}>{column.label}</th>)}<th className="px-4 py-3 text-left">Thao tác</th></tr></thead>
          <tbody>{loading ? <tr><td colSpan={productTableColumns.length + 1} className="py-14 text-center text-zinc-400">Đang tải sản phẩm…</td></tr> : products.length === 0 ? <tr><td colSpan={productTableColumns.length + 1} className="py-14 text-center text-zinc-400">Chưa có sản phẩm.</td></tr> : products.map((product) => {
            const variantAction = productVariantAction(product);
            return <tr key={product.id} className="border-t border-zinc-800">
              {productTableColumns.map((column) => <td key={column.key} className="px-4 py-3" style={column.minWidth ? { minWidth: column.minWidth } : undefined}>{column.key === "imageUrl" ? (product.imageUrl ? <img src={product.imageUrl} alt={product.name} className="h-10 w-10 rounded-lg object-cover" /> : "—") : product[column.key] ?? "—"}</td>)}
              <td className="px-4 py-3"><div className="flex items-center gap-3"><Button type="button" onClick={() => setProductDialog(createProductViewDialog(product))} title="Xem" aria-label={`Xem ${product.name || product.code || product.id}`} className="text-blue-400"><Eye size={17} /></Button><Button permission="PRODUCT_VARIANT_VIEW" type="button" onClick={() => navigate(variantAction.to)} title={variantAction.title} aria-label={variantAction.ariaLabel} className="text-cyan-400"><Link size={17} /></Button></div></td>
            </tr>;
          })}</tbody>
        </table></div>
        <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
      </div>
      {productDialog && <ProductDialog mode={productDialog.mode} product={productDialog.product} onClose={() => setProductDialog(null)} renderDetails={(product, mode) => <ProductDetailTabs key={product.id} product={product} mode={mode} />} />}
  </section>;
}
