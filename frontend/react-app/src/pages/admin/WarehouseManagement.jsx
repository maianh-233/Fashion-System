import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Warehouse } from "lucide-react";
import { requestAdmin } from "../../api/auth/adminSession";
import { useAdminPermissions } from "../../contexts/AdminPermissionsContext";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import AdminDetailDialog from "../../components/admin/common/AdminDetailDialog";
import Pagination from "../../components/common/Pagination";
import Button from "../../components/common/Button";
import StoreSelector from "../../components/common/StoreSelector";
import useStoreOptions from "../../hooks/useStoreOptions";
import WarehouseReceiptEditor from "./WarehouseReceiptEditor";
import { ISSUE_TYPES, RECEIPT_STATUSES, receiptDateParams } from "./warehouseLogic";

const control = "rounded-xl border border-zinc-700 bg-zinc-800 px-3 py-2 text-zinc-100";
const date = value => value ? new Date(value).toLocaleString("vi-VN") : "—";
const number = value => Number(value || 0).toLocaleString("vi-VN");
const tabs = [["overview", "Tổng quan", "INVENTORY_VIEW"], ["inventory", "Tồn kho", "INVENTORY_VIEW"], ["import", "Phiếu nhập", "IMPORT_RECEIPT_VIEW"], ["export", "Phiếu xuất", "EXPORT_RECEIPT_VIEW"], ["history", "Lịch sử kho", "INVENTORY_VIEW"]];
const historyHeaders = ["Thời gian", "Sản phẩm / SKU", "Loại giao dịch", "Số lượng", "Offline trước → sau", "Online trước → sau", "Người thực hiện / Tham chiếu"];
function Table({ headers, children }) {
  return <div className="overflow-x-auto rounded-2xl border border-zinc-800"><table className="w-full min-w-[760px] text-left text-sm"><thead className="bg-zinc-800 text-zinc-300"><tr>{headers.map(h => <th className="p-3" key={h}>{h}</th>)}</tr></thead><tbody className="divide-y divide-zinc-800">{children}</tbody></table></div>;
}
function TransactionRows({ rows }) {
  return rows.map(tx => <tr key={tx.id}><td className="p-3">{date(tx.createdAt)}</td><td className="p-3">{tx.productName || tx.sku || tx.productVariantId}<div className="text-xs text-zinc-400">{tx.sku}</div></td><td className="p-3">{tx.transactionType}<div className="text-xs text-zinc-400">{tx.fromChannel || "—"} → {tx.toChannel || "—"}</div></td><td className="p-3">{tx.quantity}</td><td className="p-3">{tx.beforeOffline ?? "—"} → {tx.afterOffline ?? "—"}</td><td className="p-3">{tx.beforeOnline ?? "—"} → {tx.afterOnline ?? "—"}</td><td className="p-3">{tx.createdBy || "—"}<div className="text-xs text-zinc-400">{tx.referenceId || "—"}</div></td></tr>);
}
function StockDetail({ row, storeId, onClose }) {
  const [history, setHistory] = useState({ content: [] });
  const [page, setPage] = useState(1);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    const controller = new AbortController();
    requestAdmin(`/api/inventory/transactions?${new URLSearchParams({ storeId, variantId: row.productVariantId, page: page - 1, size: 10, sort: "createdAt,desc" })}`, { signal: controller.signal })
      .then(setHistory).catch(e => { if (!controller.signal.aborted) setError(e.message); }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [storeId, row.productVariantId, page]);
  return <AdminDetailDialog open size="xl" title={row.productName || "Chi tiết tồn kho"} description={`${row.sku || ""} · ${row.variantName || ""}`} onClose={onClose} showFooter><div className="space-y-4"><p>Offline: <strong>{number(row.offlineQuantity)}</strong> · Online: <strong>{number(row.onlineQuantity)}</strong> · Tổng: <strong>{number(row.totalQuantity)}</strong></p><h3 className="font-semibold">Lịch sử biến thể</h3>{error && <p role="alert" className="text-red-300">{error}</p>}{loading ? <p>Đang tải…</p> : history.content.length ? <Table headers={historyHeaders}><TransactionRows rows={history.content} /></Table> : <p className="text-zinc-400">Chưa có giao dịch.</p>}<Pagination currentPage={page} totalPages={Math.max(1, history.totalPages || 1)} onPageChange={p => { if (p !== page) { setLoading(true); setError(""); setPage(p); } }} /></div></AdminDetailDialog>;
}
function StoreWorkspace({ store, tab, setTab, allowedTabs }) {
  const { hasPermission } = useAdminPermissions();
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [categories, setCategories] = useState([]);
  const [page, setPage] = useState(1);
  const [revision, setRevision] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [receipt, setReceipt] = useState(null);
  const [stock, setStock] = useState(null);
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const isReceipt = tab === "import" || tab === "export";
  useEffect(() => {
    if (tab !== "inventory") return;
    const controller = new AbortController();
    requestAdmin(`/api/inventory/categories?${new URLSearchParams({ storeId: store.id })}`, { signal: controller.signal })
      .then(result => setCategories(Array.isArray(result) ? result : result.content || [])).catch(e => { if (!controller.signal.aborted) setError(e.message); });
    return () => controller.abort();
  }, [store.id, tab]);
  useEffect(() => {
    const controller = new AbortController();
    const params = new URLSearchParams({ storeId: store.id, page: page - 1, size: 10, sort: tab === "inventory" ? "updatedAt,desc" : "createdAt,desc" });
    if (keyword) params.set("keyword", keyword);
    if (tab === "inventory") { params.set("stockStatus", status || "ALL"); if (categoryId) params.set("categoryId", categoryId); }
    if (isReceipt && status) params.set("status", status);
    if (isReceipt) Object.entries(receiptDateParams(fromDate, toDate)).forEach(([key, value]) => params.set(key, value));
    const endpoint = tab === "overview" ? "/api/inventory/statistics" : tab === "inventory" ? "/api/inventory/balances" : tab === "history" ? "/api/inventory/transactions" : `/api/${tab}-receipts`;
    const timer = setTimeout(() => requestAdmin(`${endpoint}?${params}`, { signal: controller.signal })
      .then(result => { if (!controller.signal.aborted) setData(result); })
      .catch(e => { if (!controller.signal.aborted) setError(e.message || "Không thể tải dữ liệu kho."); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); }), keyword ? 250 : 0);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [store.id, tab, page, keyword, status, categoryId, revision, fromDate, toDate, isReceipt]);
  const rows = data?.content || [];
  const totals = data?.totals || {};
  const beginLoad = () => { setLoading(true); setError(""); };
  const changeFilter = setter => e => { beginLoad(); setter(e.target.value); setPage(1); };
  const refresh = () => { beginLoad(); setRevision(r => r + 1); };
  return <div className="space-y-5">
    <nav aria-label="Chức năng kho" className="flex flex-wrap gap-2">{allowedTabs.map(([id, label]) => <button key={id} type="button" aria-current={tab === id ? "page" : undefined} className={`${control} ${tab === id ? "border-amber-400 text-amber-300" : ""}`} onClick={() => setTab(id)}>{label}</button>)}</nav>
    <section className="space-y-4 rounded-3xl border border-zinc-800 bg-zinc-900 p-4"><div className="flex flex-wrap items-center gap-3">
      {["inventory", "import", "export"].includes(tab) && <input className={`${control} min-w-[240px] flex-1`} aria-label="Tìm kiếm" placeholder={isReceipt ? "Tìm mã phiếu…" : "Tìm tên sản phẩm, SKU…"} value={keyword} onChange={changeFilter(setKeyword)} />}
      {tab === "inventory" && <><select className={control} aria-label="Danh mục" value={categoryId} onChange={changeFilter(setCategoryId)}><option value="">Tất cả danh mục</option>{categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select><select className={control} aria-label="Tình trạng tồn" value={status} onChange={changeFilter(setStatus)}><option value="">Tất cả tồn kho</option><option value="LOW">Sắp hết (1–5)</option><option value="OUT">Hết hàng</option><option value="IN">Còn hàng</option></select></>}
      {isReceipt && <><select className={control} aria-label="Trạng thái phiếu" value={status} onChange={changeFilter(setStatus)}><option value="">Tất cả trạng thái</option>{Object.entries(RECEIPT_STATUSES).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select><label className="text-xs text-zinc-400">Từ ngày <input className={control} type="date" value={fromDate} onChange={changeFilter(setFromDate)} /></label><label className="text-xs text-zinc-400">Đến ngày <input className={control} type="date" value={toDate} min={fromDate} onChange={changeFilter(setToDate)} /></label>{hasPermission(`${tab === "import" ? "IMPORT" : "EXPORT"}_RECEIPT_CREATE`) && <Button className={`${control} bg-amber-600`} onClick={() => setReceipt({ id: null })}>+ Tạo phiếu {tab === "import" ? "nhập" : "xuất"}</Button>}</>}
      <Button className={control} disabled={loading} onClick={refresh}>Làm mới</Button>
    </div>
    {error && <p role="alert" className="rounded-xl bg-red-500/10 p-3 text-red-300">{error}</p>}
    {loading ? <p role="status" className="py-10 text-center text-zinc-400">Đang tải dữ liệu cửa hàng…</p> : tab === "overview" && data ? <><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{[["Tổng SKU", totals.totalSku], ["Tồn Offline", totals.offlineQuantity], ["Tồn Online", totals.onlineQuantity], ["Tổng tồn", totals.totalQuantity ?? totals.availableQuantity], ["SKU sắp hết", totals.lowStockSku], ["SKU hết hàng", totals.outOfStockSku]].map(([label, value]) => <div key={label} className="rounded-2xl border border-zinc-800 bg-zinc-950 p-5"><p className="text-sm text-zinc-400">{label}</p><p className="mt-2 text-3xl font-semibold text-amber-300">{number(value)}</p></div>)}</div><p className="text-sm text-zinc-400">Số liệu thuộc riêng {store.name}. Phiếu chưa hoàn tất chưa làm thay đổi tồn kho.</p></> : !error && rows.length === 0 ? <p className="py-12 text-center text-zinc-400">{isReceipt ? "Chưa có phiếu phù hợp. Tạo phiếu mới hoặc thay đổi bộ lọc." : "Không có dữ liệu phù hợp với bộ lọc."}</p> : !error && <>
      {tab === "inventory" && <Table headers={["Sản phẩm / SKU", "Biến thể", "Offline", "Online", "Tổng tồn", "Trạng thái", "Cập nhật", "Chi tiết"]}>{rows.map(row => <tr key={row.productVariantId}><td className="p-3">{row.productName}<div className="text-xs text-zinc-400">{row.sku}</div></td><td className="p-3">{row.variantName || "—"}</td><td className="p-3">{number(row.offlineQuantity)}</td><td className="p-3">{number(row.onlineQuantity)}</td><td className="p-3 font-semibold">{number(row.totalQuantity)}</td><td className="p-3"><span className={row.totalQuantity === 0 ? "text-red-300" : row.totalQuantity <= 5 ? "text-amber-300" : "text-emerald-300"}>{row.totalQuantity === 0 ? "Hết hàng" : row.totalQuantity <= 5 ? "Sắp hết" : "Còn hàng"}</span></td><td className="p-3">{date(row.updatedAt)}</td><td className="p-3"><button type="button" className="text-amber-300" onClick={() => setStock(row)}>Xem lịch sử</button></td></tr>)}</Table>}
      {isReceipt && <Table headers={["Mã phiếu", tab === "import" ? "Nhà cung cấp" : "Loại xuất", "Người tạo", "Ngày tạo", "Xác nhận", "Hoàn tất", "Trạng thái", "Số lượng", ...(tab === "import" ? ["Tổng tiền"] : []), "Thao tác"]}>{rows.map(row => <tr key={row.id}><td className="p-3 font-medium">{row.receiptCode || row.issueCode}</td><td className="p-3">{tab === "import" ? row.supplierName || row.supplierId || "—" : ISSUE_TYPES[row.issueType] || row.issueType}</td><td className="p-3">{row.receivedByName || row.issuedByName || row.receivedBy || row.issuedBy || "—"}</td><td className="p-3">{date(row.createdAt)}</td><td className="p-3">{row.approvedByName || row.approvedBy || "—"}<div className="text-xs text-zinc-400">{date(row.confirmedAt)}</div></td><td className="p-3">{row.completedByName || row.completedBy || "—"}<div className="text-xs text-zinc-400">{date(row.completedAt)}</div></td><td className="p-3"><span className="rounded-full bg-amber-500/10 px-2 py-1 text-amber-300">{RECEIPT_STATUSES[row.status] || row.status}</span></td><td className="p-3">{number(row.totalQuantity)}</td>{tab === "import" && <td className="p-3">{number(row.totalAmount)} ₫</td>}<td className="p-3"><button type="button" className="text-amber-300" onClick={() => setReceipt(row)}>Mở phiếu</button></td></tr>)}</Table>}
      {tab === "history" && <Table headers={historyHeaders}><TransactionRows rows={rows} /></Table>}
    </>}
    {tab !== "overview" && <Pagination currentPage={page} totalPages={Math.max(1, data?.totalPages || 1)} onPageChange={p => { if (p !== page) { beginLoad(); setPage(p); } }} />}</section>
    {receipt && <WarehouseReceiptEditor key={receipt.id || "new"} storeId={store.id} storeName={store.name} kind={tab} receiptId={receipt.id} onClose={() => setReceipt(null)} onChanged={refresh} />}
    {stock && <StockDetail row={stock} storeId={store.id} onClose={() => setStock(null)} />}
  </div>;
}
export default function WarehouseManagement({ initialTab = "overview" }) {
  const { isGlobal, isStore, currentStoreId, currentStoreName, hasPermission, loading: permissionsLoading } = useAdminPermissions();
  const [params, setParams] = useSearchParams();
  const { stores, loading, error, reload } = useStoreOptions({ enabled: !permissionsLoading });
  const allowedTabs = tabs.filter(([, , permission]) => hasPermission(permission));
  const wantedTab = params.get("tab") || initialTab;
  const tab = allowedTabs.some(([id]) => id === wantedTab) ? wantedTab : allowedTabs[0]?.[0];
  const storeId = isStore ? currentStoreId : params.get("storeId");
  const store = stores.find(s => s.id === storeId);
  const selectStore = id => { const next = new URLSearchParams(params); if (id) next.set("storeId", id); else next.delete("storeId"); setParams(next); };
  const setTab = id => { const next = new URLSearchParams(params); next.set("tab", id); if (storeId) next.set("storeId", storeId); setParams(next); };
  return <div className="admin-catalog-page admin-catalog-page--inventory space-y-6 text-zinc-100">
    <AdminCatalogPageHeader icon={Warehouse} eyebrow="Vận hành kho" title={store ? `Kho · ${store.name}` : "Quản lý kho theo cửa hàng"} description="Theo dõi tồn Offline / Online, nhập xuất hàng và lịch sử biến động của từng cửa hàng." status={isStore ? currentStoreName : "Chọn cửa hàng để làm việc"} />
    {loading ? <p role="status">Đang tải cửa hàng…</p> : error ? <div><p role="alert" className="text-red-300">{error}</p><Button className={control} onClick={reload}>Thử lại</Button></div> : <>
      <StoreSelector stores={stores} value={store?.id || ""} onChange={selectStore} required
        readOnly={isStore} label="Cửa hàng" placeholder="Chọn một cửa hàng"
        className={`${control} min-w-[260px] ${isGlobal ? "" : "opacity-80"}`} />
      {store && tab ? <StoreWorkspace key={`${store.id}:${tab}`} store={store} tab={tab} setTab={setTab} allowedTabs={allowedTabs} /> : <div className="rounded-3xl border border-zinc-800 bg-zinc-900 p-8"><h2 className="mb-3 text-lg font-semibold">{stores.length ? "Chọn cửa hàng để xem và vận hành kho" : "Không có cửa hàng khả dụng"}</h2>{isGlobal && <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{stores.map(s => <button type="button" key={s.id} className="rounded-2xl border border-zinc-700 p-5 text-left hover:border-amber-400" onClick={() => selectStore(s.id)}><Warehouse className="mb-3 text-amber-300" size={22} /><span className="font-medium">{s.name}</span><p className="mt-1 text-sm text-zinc-400">Mở kho cửa hàng →</p></button>)}</div>}{storeId && !store && <p className="text-amber-300">Cửa hàng không còn hoạt động hoặc không thuộc phạm vi truy cập.</p>}</div>}
    </>}
  </div>;
}
