import { useCallback, useEffect, useMemo, useState } from "react";
import { AlertTriangle, Archive, Boxes, Eye, RefreshCw, Settings, Warehouse } from "lucide-react";
import Button from "../../components/common/Button";
import Pagination from "../../components/common/Pagination";
import AdminDetailDialog from "../../components/admin/common/AdminDetailDialog";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import { useAdminPermissions } from "../../contexts/AdminPermissionsContext";
import { requestAdmin } from "../../hooks/auth/adminSession";

const PAGE_SIZE = 10;
const shortId = (value) => value ? value.slice(0, 8).toUpperCase() : "—";
const statusFor = (quantity) => quantity === 0
  ? { label: "Hết hàng", className: "text-red-300 bg-red-500/15" }
  : quantity <= 5
    ? { label: "Sắp hết", className: "text-amber-300 bg-amber-500/15" }
    : { label: "Còn hàng", className: "text-emerald-300 bg-emerald-500/15" };

export default function InventoryManagement() {
  const { isGlobal, isStore, currentStoreId, currentStoreName } = useAdminPermissions();
  const [rows, setRows] = useState([]);
  const [stats, setStats] = useState(null);
  const [selectedStoreId, setSelectedStoreId] = useState("");
  const [lowStockOnly, setLowStockOnly] = useState(false);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const effectiveStoreId = isStore ? currentStoreId : selectedStoreId || null;

  const load = useCallback(async () => {
    if (!isGlobal && !isStore) return;
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({ page: String(page - 1), size: String(PAGE_SIZE), sort: "updatedAt,desc" });
      const statsParams = new URLSearchParams({ lowStockThreshold: "5" });
      if (effectiveStoreId) {
        params.set("storeId", effectiveStoreId);
        statsParams.set("storeId", effectiveStoreId);
      }
      if (lowStockOnly) params.set("lowStockThreshold", "5");
      const [balances, statistics] = await Promise.all([
        requestAdmin(`/api/inventory/balances?${params}`),
        requestAdmin(`/api/inventory/statistics?${statsParams}`),
      ]);
      setRows(Array.isArray(balances?.content) ? balances.content : []);
      setTotalPages(Math.max(1, balances?.totalPages || 1));
      setStats(statistics);
    } catch (requestError) {
      setRows([]);
      setStats(null);
      setError(requestError.message || "Không thể tải dữ liệu tồn kho.");
    } finally {
      setLoading(false);
    }
  }, [effectiveStoreId, isGlobal, isStore, lowStockOnly, page]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => { setPage(1); }, [effectiveStoreId, lowStockOnly]);

  const storeOptions = useMemo(
    () => stats?.byStore?.map((item) => ({ id: item.storeId, name: item.storeName || `Store ${shortId(item.storeId)}` })) || [],
    [stats],
  );
  const totals = stats?.totals || {};

  const openDetail = async (row) => {
    setSelected({ ...row, transactions: [], loadingTransactions: true });
    try {
      const params = new URLSearchParams({ storeId: row.storeId, variantId: row.productVariantId, page: "0", size: "20", sort: "createdAt,desc" });
      const history = await requestAdmin(`/api/inventory/transactions?${params}`);
      setSelected((current) => current && ({ ...current, transactions: history?.content || [], loadingTransactions: false }));
    } catch (requestError) {
      setSelected((current) => current && ({ ...current, loadingTransactions: false, historyError: requestError.message }));
    }
  };

  const adjust = async (row) => {
    const raw = window.prompt("Nhập số lượng điều chỉnh (số âm để giảm):", "1");
    if (raw === null) return;
    const quantityDelta = Number(raw);
    if (!Number.isInteger(quantityDelta) || quantityDelta === 0) {
      window.alert("Số lượng điều chỉnh phải là số nguyên khác 0.");
      return;
    }
    try {
      await requestAdmin("/api/inventory/adjustments", {
        method: "POST",
        body: { storeId: row.storeId, productVariantId: row.productVariantId, quantityDelta },
      });
      await load();
    } catch (requestError) {
      window.alert(requestError.message || "Điều chỉnh tồn kho thất bại.");
    }
  };

  const cards = [
    ["Tổng SKU", totals.totalSku || 0, Boxes],
    ["Tồn khả dụng", totals.availableQuantity || 0, Warehouse],
    ["SKU sắp hết", totals.lowStockSku || 0, AlertTriangle],
    ["SKU hết hàng", totals.outOfStockSku || 0, Archive],
  ];

  return <div className="admin-catalog-page admin-catalog-page--inventory text-zinc-100"><div className="space-y-6">
    <AdminCatalogPageHeader icon={Warehouse} eyebrow="Vận hành kho" title="Quản lý tồn kho" description="Dữ liệu tồn kho được phân trang và giới hạn Store trực tiếp tại backend." status={isStore && currentStoreName ? `Cửa hàng: ${currentStoreName}` : "Toàn chuỗi"} />

    <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">{cards.map(([label, value, Icon]) => <div key={label} className="rounded-3xl border border-zinc-800 bg-zinc-900 p-5"><div className="flex items-center justify-between"><div><p className="text-sm text-zinc-400">{label}</p><p className="mt-2 text-3xl font-semibold">{Number(value).toLocaleString("vi-VN")}</p></div><Icon className="text-amber-300" size={22} /></div></div>)}</section>

    <section className="rounded-3xl border border-zinc-800 bg-zinc-900 p-4"><div className="flex flex-wrap items-center gap-3">
      {isGlobal && <select value={selectedStoreId} onChange={(event) => setSelectedStoreId(event.target.value)} className="h-11 rounded-2xl border border-zinc-700 bg-zinc-800 px-4"><option value="">Tất cả cửa hàng</option>{storeOptions.map((store) => <option key={store.id} value={store.id}>{store.name}</option>)}</select>}
      <label className="inline-flex items-center gap-2 text-sm text-zinc-300"><input type="checkbox" checked={lowStockOnly} onChange={(event) => setLowStockOnly(event.target.checked)} /> Chỉ SKU tồn thấp (≤ 5)</label>
      <Button type="button" onClick={load} className="inline-flex h-11 items-center gap-2 rounded-2xl bg-zinc-800 px-4"><RefreshCw size={16} /> Làm mới</Button>
    </div>{error && <p className="mt-3 text-sm text-red-300">{error}</p>}</section>

    <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900"><div className="overflow-x-auto"><table className="w-full min-w-[820px] text-sm">
      <thead className="bg-zinc-800/70 text-zinc-300"><tr><th className="px-4 py-3 text-left">Variant</th>{isGlobal && <th className="px-4 py-3 text-left">Store</th>}<th className="px-4 py-3 text-left">Khả dụng</th><th className="px-4 py-3 text-left">Đã giữ</th><th className="px-4 py-3 text-left">Hư hỏng</th><th className="px-4 py-3 text-left">Trạng thái</th><th className="px-4 py-3 text-left">Thao tác</th></tr></thead>
      <tbody>
        {!loading && rows.length === 0 && <tr><td colSpan={isGlobal ? 7 : 6} className="py-14 text-center text-zinc-400">Không có dữ liệu tồn kho.</td></tr>}
        {loading && <tr><td colSpan={isGlobal ? 7 : 6} className="py-14 text-center text-zinc-400">Đang tải…</td></tr>}
        {!loading && rows.map((row) => { const status = statusFor(row.availableQuantity); return <tr key={`${row.storeId}-${row.productVariantId}`} className="border-t border-zinc-800"><td className="px-4 py-3 font-medium">{shortId(row.productVariantId)}</td>{isGlobal && <td className="px-4 py-3 text-zinc-300">{shortId(row.storeId)}</td>}<td className="px-4 py-3">{row.availableQuantity}</td><td className="px-4 py-3">{row.reservedQuantity}</td><td className="px-4 py-3">{row.damagedQuantity}</td><td className="px-4 py-3"><span className={`rounded-full px-2.5 py-1 text-xs ${status.className}`}>{status.label}</span></td><td className="px-4 py-3"><div className="flex gap-3"><Button type="button" onClick={() => openDetail(row)} title="Xem chi tiết"><Eye size={18} /></Button><Button permission="INVENTORY_ADJUST" type="button" onClick={() => adjust(row)} title="Điều chỉnh"><Settings size={18} /></Button></div></td></tr>; })}
      </tbody>
    </table></div><Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} /></section>
  </div>

  {selected && <AdminDetailDialog open title={`Tồn kho Variant ${shortId(selected.productVariantId)}`} description={isStore ? `Cửa hàng: ${currentStoreName}` : `Store ${shortId(selected.storeId)}`} onClose={() => setSelected(null)} size="large" showFooter>
    <div className="grid grid-cols-3 gap-3 text-sm"><p className="rounded-2xl bg-zinc-800 p-3">Khả dụng: <strong>{selected.availableQuantity}</strong></p><p className="rounded-2xl bg-zinc-800 p-3">Đã giữ: <strong>{selected.reservedQuantity}</strong></p><p className="rounded-2xl bg-zinc-800 p-3">Hư hỏng: <strong>{selected.damagedQuantity}</strong></p></div>
    <h3 className="mt-6 mb-3 font-semibold">Lịch sử giao dịch</h3>{selected.loadingTransactions && <p className="text-zinc-400">Đang tải…</p>}{selected.historyError && <p className="text-red-300">{selected.historyError}</p>}<div className="space-y-2">{selected.transactions.map((tx) => <div key={tx.id} className="flex justify-between rounded-xl border border-zinc-800 p-3 text-sm"><span>{tx.transactionType}</span><span>{tx.quantity > 0 ? "+" : ""}{tx.quantity} → {tx.balanceAfter}</span></div>)}</div>
  </AdminDetailDialog>}
  </div>;
}
