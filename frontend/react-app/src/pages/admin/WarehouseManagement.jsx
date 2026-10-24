import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  Warehouse, Search, Plus, Eye, PenSquare, RotateCcw, RefreshCw, History,
  LayoutDashboard, Boxes, PackagePlus, PackageMinus, FileText, Clock3, CircleCheck, PackageCheck,
} from "lucide-react";
import { requestAdmin } from "../../api/http/adminSession";
import { useAdminPermissions } from "../../contexts/AdminPermissionsContext";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import AdminDetailDialog from "../../components/admin/common/AdminDetailDialog";
import Pagination from "../../components/common/Pagination";
import Button from "../../components/common/Button";
import StoreSelector from "../../components/common/StoreSelector";
import useStoreOptions from "../../hooks/useStoreOptions";
import WarehouseReceiptEditor from "./WarehouseReceiptEditor";
import { ISSUE_TYPES, RECEIPT_STATUSES, RECEIPT_STATUS_STYLES, isReceiptEditable, receiptDateParams } from "./warehouseLogic";

const control = "bg-zinc-800 border border-zinc-700 rounded-2xl py-3 px-4 text-sm";
const ALIGN = { left: "text-left", center: "text-center", right: "text-right" };
const date = value => value ? new Date(value).toLocaleString("vi-VN") : "—";
const number = value => Number(value || 0).toLocaleString("vi-VN");
const tabs = [
  ["overview", "Tổng quan", "INVENTORY_VIEW", LayoutDashboard],
  ["inventory", "Tồn kho", "INVENTORY_VIEW", Boxes],
  ["import", "Phiếu nhập", "IMPORT_RECEIPT_VIEW", PackagePlus],
  ["export", "Phiếu xuất", "EXPORT_RECEIPT_VIEW", PackageMinus],
  ["history", "Lịch sử kho", "INVENTORY_VIEW", History],
];
const historyHeaders = ["Thời gian", "Sản phẩm / SKU", "Loại giao dịch", "Số lượng", "Offline trước → sau", "Online trước → sau", "Người thực hiện / Tham chiếu"];
const RECEIPT_STAT_CARDS = [
  { key: "total", label: "Tổng phiếu", icon: FileText, className: "text-blue-400" },
  { key: "PENDING_CONFIRMATION", label: "Chờ xác nhận", icon: Clock3, className: "text-amber-400" },
  { key: "CONFIRMED", label: "Đã xác nhận", icon: CircleCheck, className: "text-purple-400" },
  { key: "COMPLETED", label: "Hoàn thành", icon: PackageCheck, className: "text-emerald-400" },
];

function StatusBadge({ status }) {
  return <span className={`px-4 py-1 rounded-full text-xs whitespace-nowrap ${RECEIPT_STATUS_STYLES[status] || "bg-zinc-700"}`}>{RECEIPT_STATUSES[status] || status}</span>;
}

function StatCards({ cards, columns = "lg:grid-cols-4" }) {
  return <div className={`admin-catalog-stats grid grid-cols-1 md:grid-cols-2 ${columns} gap-6`}>
    {cards.map(({ key, label, value, icon: Icon, className }) => <div key={key} className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
      <div className="flex justify-between items-start">
        <div><p className="text-zinc-400">{label}</p><p className={`text-4xl font-bold mt-2 ${className}`}>{value ?? "–"}</p></div>
        <Icon size={40} className={className} />
      </div>
    </div>)}
  </div>;
}

function DataTable({ title, total, headers, loading, error, emptyText, onRetry, footer, children }) {
  const colSpan = headers.length;
  return <div className="admin-catalog-table bg-zinc-900 rounded-3xl overflow-hidden border border-zinc-800">
    <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-950">
      <h3 className="font-semibold text-lg">{title}</h3>
      {total !== undefined && <p className="text-sm text-zinc-400">Tìm thấy:<span className="font-medium text-white ml-1">{number(total)}</span></p>}
    </div>
    <div className="overflow-x-auto">
      <table className="w-full min-w-[760px]">
        <thead><tr className="border-b border-zinc-800 text-zinc-400 text-sm">{headers.map(([label, align = "left"]) => <th key={label} className={`${ALIGN[align]} py-5 px-6 font-normal`}>{label}</th>)}</tr></thead>
        <tbody className="divide-y divide-zinc-800 text-sm">
          {loading && <tr><td colSpan={colSpan} className="px-6 py-10 text-center text-zinc-400">Đang tải dữ liệu...</td></tr>}
          {!loading && error && <tr><td colSpan={colSpan} className="px-6 py-10 text-center text-red-400">{error}{onRetry && <Button onClick={onRetry} className="ml-3 text-amber-400 underline" variant="unstyled">Thử lại</Button>}</td></tr>}
          {!loading && !error && !children?.length && <tr><td colSpan={colSpan} className="px-6 py-10 text-center text-zinc-400">{emptyText}</td></tr>}
          {!loading && !error && children}
        </tbody>
      </table>
    </div>
    {footer}
  </div>;
}

const transactionRows = rows => rows.map(tx => <tr key={tx.id} className="hover:bg-zinc-800 transition-colors">
  <td className="px-6 py-5 whitespace-nowrap">{date(tx.createdAt)}</td>
  <td className="px-6 py-5 font-medium">{tx.productName || tx.sku || "—"}<span className="block text-xs text-zinc-400">{tx.sku}</span></td>
  <td className="px-6 py-5">{tx.transactionType}<span className="block text-xs text-zinc-400">{tx.fromChannel || "—"} → {tx.toChannel || "—"}</span></td>
  <td className="px-6 py-5 text-center">{number(tx.quantity)}</td>
  <td className="px-6 py-5 text-center whitespace-nowrap">{tx.beforeOffline ?? "—"} → {tx.afterOffline ?? "—"}</td>
  <td className="px-6 py-5 text-center whitespace-nowrap">{tx.beforeOnline ?? "—"} → {tx.afterOnline ?? "—"}</td>
  <td className="px-6 py-5">{tx.createdByName || "—"}<span className="block text-xs text-zinc-400">{tx.referenceCode || "—"}</span></td>
</tr>);
const historyColumns = historyHeaders.map((label, i) => [label, [3, 4, 5].includes(i) ? "center" : "left"]);

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
  return <AdminDetailDialog open size="xl" title={row.productName || "Chi tiết tồn kho"} description={`${row.sku || ""} · ${row.variantName || ""}`} onClose={onClose} showFooter>
    <div className="space-y-6">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {[["Tồn Offline", row.offlineQuantity], ["Tồn Online", row.onlineQuantity], ["Tổng tồn", row.totalQuantity]].map(([label, value]) => <div key={label} className="rounded-2xl border border-zinc-800 bg-zinc-950 p-4"><p className="text-xs text-zinc-400">{label}</p><p className="mt-1 text-2xl font-semibold text-amber-300">{number(value)}</p></div>)}
      </div>
      <DataTable title="Lịch sử biến thể" total={history.totalElements} headers={historyColumns} loading={loading} error={error} emptyText="Chưa có giao dịch."
        footer={<Pagination currentPage={page} totalPages={Math.max(1, history.totalPages || 1)} onPageChange={p => { if (p !== page) { setLoading(true); setError(""); setPage(p); } }} />}>
        {transactionRows(history.content || [])}
      </DataTable>
    </div>
  </AdminDetailDialog>;
}

function StoreWorkspace({ store, storeSelect, tab, setTab, allowedTabs, linkedReceiptId, clearLinkedReceipt }) {
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
  const [receiptStats, setReceiptStats] = useState({});
  const [openedLink, setOpenedLink] = useState(null);
  const isReceipt = tab === "import" || tab === "export";
  // Link từ thông báo (?receipt=<id>) mở thẳng phiếu; đóng phiếu thì bỏ tham số để tải lại trang không mở lại.
  if (isReceipt && linkedReceiptId && openedLink !== linkedReceiptId) {
    setOpenedLink(linkedReceiptId);
    setReceipt({ id: linkedReceiptId, mode: "view" });
  } else if (!linkedReceiptId && openedLink) {
    setOpenedLink(null);
  }
  const closeReceipt = () => { setReceipt(null); if (linkedReceiptId) clearLinkedReceipt(); };
  const isImport = tab === "import";
  const prefix = isImport ? "IMPORT_RECEIPT" : "EXPORT_RECEIPT";
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
  useEffect(() => {
    if (!isReceipt) return;
    const controller = new AbortController();
    const count = value => requestAdmin(`/api/${tab}-receipts?${new URLSearchParams({ storeId: store.id, size: 1, ...(value ? { status: value } : {}) })}`, { signal: controller.signal })
      .then(result => result?.totalElements ?? 0);
    Promise.all(RECEIPT_STAT_CARDS.map(card => count(card.key === "total" ? "" : card.key)))
      .then(values => setReceiptStats(Object.fromEntries(RECEIPT_STAT_CARDS.map((card, i) => [card.key, values[i]]))))
      .catch(() => { if (!controller.signal.aborted) setReceiptStats({}); });
    return () => controller.abort();
  }, [store.id, tab, revision, isReceipt]);
  const rows = data?.content || [];
  const totals = data?.totals || {};
  const beginLoad = () => { setLoading(true); setError(""); };
  const changeFilter = setter => e => { beginLoad(); setter(e.target.value); setPage(1); };
  const refresh = () => { beginLoad(); setRevision(r => r + 1); };
  const resetFilters = () => { beginLoad(); setKeyword(""); setStatus(""); setCategoryId(""); setFromDate(""); setToDate(""); setPage(1); };
  const pagination = <Pagination currentPage={page} totalPages={Math.max(1, data?.totalPages || 1)} onPageChange={p => { if (p !== page) { beginLoad(); setPage(p); } }} />;
  const tableProps = { total: data?.totalElements, loading, error, onRetry: refresh, footer: pagination };
  const receiptHeaders = [["Mã phiếu"], [isImport ? "Nhà cung cấp" : "Loại xuất"], ["Người tạo"], ["Ngày tạo", "center"], ["Xác nhận"], ["Hoàn tất"], ["Trạng thái", "center"], ["Số lượng", "center"], ...(isImport ? [["Tổng tiền", "right"]] : []), ["Thao tác", "center"]];
  return <>
    <div className="admin-tabs" role="tablist" aria-label="Chức năng kho">
      {allowedTabs.map(([id, label, , Icon]) => <Button key={id} variant="ghost" role="tab" aria-selected={tab === id} className={`admin-tab ${tab === id ? "is-active" : ""}`} onClick={() => setTab(id)}><Icon size={17} /> {label}</Button>)}
    </div>

    <div className="admin-catalog-toolbar bg-zinc-900 border border-zinc-800 rounded-3xl p-6">
      <div className="flex flex-wrap items-end gap-4">
        {storeSelect}
        {["inventory", "import", "export"].includes(tab) && <div className="relative">
          <input type="text" aria-label="Tìm kiếm" placeholder={isReceipt ? "Tìm theo mã phiếu..." : "Tìm tên sản phẩm, SKU..."} value={keyword} onChange={changeFilter(setKeyword)}
            className="bg-zinc-800 border border-zinc-700 focus:border-amber-400 rounded-2xl py-3 pl-11 pr-4 w-80 max-w-full focus:outline-none text-sm" />
          <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" />
        </div>}
        {tab === "inventory" && <>
          <select className={control} aria-label="Danh mục" value={categoryId} onChange={changeFilter(setCategoryId)}><option value="">Tất cả danh mục</option>{categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select>
          <select className={control} aria-label="Tình trạng tồn" value={status} onChange={changeFilter(setStatus)}><option value="">Tất cả tồn kho</option><option value="LOW">Sắp hết (1–5)</option><option value="OUT">Hết hàng</option><option value="IN">Còn hàng</option></select>
        </>}
        {isReceipt && <>
          <select className={control} aria-label="Trạng thái phiếu" value={status} onChange={changeFilter(setStatus)}><option value="">Tất cả trạng thái</option>{Object.entries(RECEIPT_STATUSES).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select>
          <label className="flex flex-col gap-1 text-xs text-zinc-400">Từ ngày<input type="date" value={fromDate} max={toDate || undefined} onChange={changeFilter(setFromDate)} className="bg-zinc-800 border border-zinc-700 rounded-2xl py-2.5 px-4 text-sm text-white" /></label>
          <label className="flex flex-col gap-1 text-xs text-zinc-400">Đến ngày<input type="date" value={toDate} min={fromDate || undefined} onChange={changeFilter(setToDate)} className="bg-zinc-800 border border-zinc-700 rounded-2xl py-2.5 px-4 text-sm text-white" /></label>
        </>}
        {tab !== "overview" && tab !== "history" && <Button onClick={resetFilters} className="bg-blue-500 hover:bg-blue-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"><RotateCcw size={18} /><span>Reset</span></Button>}
        <Button onClick={refresh} disabled={loading} className="bg-blue-500 hover:bg-blue-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"><RefreshCw size={18} /><span>Làm mới</span></Button>
        {isReceipt && <Button permission={`${prefix}_CREATE`} onClick={() => setReceipt({ id: null, mode: "edit" })} className="bg-amber-500 hover:bg-amber-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"><Plus size={18} /><span>Tạo phiếu {isImport ? "nhập" : "xuất"}</span></Button>}
      </div>
    </div>

    {tab === "overview" && <>
      {loading ? <p role="status" className="py-10 text-center text-zinc-400">Đang tải dữ liệu cửa hàng...</p>
        : error ? <p role="alert" className="rounded-xl border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</p>
          : <>
            <StatCards cards={[
              { key: "sku", label: "Tổng SKU", value: number(totals.totalSku), icon: Boxes, className: "text-blue-400" },
              { key: "offline", label: "Tồn Offline", value: number(totals.offlineQuantity), icon: Warehouse, className: "text-amber-400" },
              { key: "online", label: "Tồn Online", value: number(totals.onlineQuantity), icon: Warehouse, className: "text-purple-400" },
              { key: "total", label: "Tổng tồn", value: number(totals.totalQuantity ?? totals.availableQuantity), icon: PackageCheck, className: "text-emerald-400" },
              { key: "low", label: "SKU sắp hết", value: number(totals.lowStockSku), icon: Clock3, className: "text-orange-400" },
              { key: "out", label: "SKU hết hàng", value: number(totals.outOfStockSku), icon: PackageMinus, className: "text-red-400" },
            ]} columns="lg:grid-cols-3" />
            <p className="text-sm text-zinc-400">Số liệu thuộc riêng {store.name}. Phiếu chưa hoàn tất chưa làm thay đổi tồn kho.</p>
          </>}
    </>}

    {tab === "inventory" && <DataTable title="Tồn kho theo biến thể" emptyText="Không có dữ liệu phù hợp với bộ lọc." {...tableProps}
      headers={[["Sản phẩm / SKU"], ["Biến thể"], ["Offline", "center"], ["Online", "center"], ["Tổng tồn", "center"], ["Trạng thái", "center"], ["Cập nhật", "center"], ["Thao tác", "center"]]}>
      {rows.map(row => <tr key={row.productVariantId} className="hover:bg-zinc-800 transition-colors">
        <td className="px-6 py-5 font-medium">{row.productName}<span className="block text-xs text-zinc-400">{row.sku}</span></td>
        <td className="px-6 py-5">{row.variantName || "—"}</td>
        <td className="px-6 py-5 text-center">{number(row.offlineQuantity)}</td>
        <td className="px-6 py-5 text-center">{number(row.onlineQuantity)}</td>
        <td className="px-6 py-5 text-center font-semibold">{number(row.totalQuantity)}</td>
        <td className="px-6 py-5 text-center"><span className={`px-4 py-1 rounded-full text-xs whitespace-nowrap ${row.totalQuantity === 0 ? "bg-red-500/20 text-red-400" : row.totalQuantity <= 5 ? "bg-amber-500/20 text-amber-300" : "bg-emerald-500/20 text-emerald-400"}`}>{row.totalQuantity === 0 ? "Hết hàng" : row.totalQuantity <= 5 ? "Sắp hết" : "Còn hàng"}</span></td>
        <td className="px-6 py-5 text-center whitespace-nowrap">{date(row.updatedAt)}</td>
        <td className="px-6 py-5"><div className="flex items-center justify-center gap-3"><Button onClick={() => setStock(row)} className="text-blue-400 hover:text-blue-300 transition-colors" title="Xem lịch sử"><History size={18} /></Button></div></td>
      </tr>)}
    </DataTable>}

    {isReceipt && <>
      <StatCards cards={RECEIPT_STAT_CARDS.map(card => ({ ...card, value: receiptStats[card.key] }))} />
      <DataTable title={`Danh sách phiếu ${isImport ? "nhập" : "xuất"}`} emptyText="Chưa có phiếu phù hợp. Tạo phiếu mới hoặc thay đổi bộ lọc." headers={receiptHeaders} {...tableProps}>
        {rows.map(row => <tr key={row.id} className="hover:bg-zinc-800 transition-colors">
          <td className="px-6 py-5 font-medium text-amber-400 whitespace-nowrap">{row.receiptCode || row.issueCode}</td>
          <td className="px-6 py-5">{isImport ? row.supplierName || "—" : ISSUE_TYPES[row.issueType] || row.issueType}</td>
          <td className="px-6 py-5">{row.receivedByName || row.issuedByName || "—"}</td>
          <td className="px-6 py-5 text-center whitespace-nowrap">{date(row.createdAt)}</td>
          <td className="px-6 py-5">{row.approvedByName || "—"}<span className="block text-xs text-zinc-400 whitespace-nowrap">{date(row.confirmedAt)}</span></td>
          <td className="px-6 py-5">{row.completedByName || "—"}<span className="block text-xs text-zinc-400 whitespace-nowrap">{date(row.completedAt)}</span></td>
          <td className="px-6 py-5 text-center"><StatusBadge status={row.status} /></td>
          <td className="px-6 py-5 text-center">{number(row.totalQuantity)}</td>
          {isImport && <td className="px-6 py-5 text-right whitespace-nowrap">{number(row.totalAmount)} ₫</td>}
          <td className="px-6 py-5">
            <div className="flex items-center justify-center gap-3">
              <Button onClick={() => setReceipt({ id: row.id, mode: "view" })} className="text-blue-400 hover:text-blue-300 transition-colors" title="Xem"><Eye size={18} /></Button>
              {isReceiptEditable(row.status) && <Button permission={`${prefix}_UPDATE`} onClick={() => setReceipt({ id: row.id, mode: "edit" })} className="text-amber-400 hover:text-amber-300 transition-colors" title="Sửa"><PenSquare size={18} /></Button>}
            </div>
          </td>
        </tr>)}
      </DataTable>
    </>}

    {tab === "history" && <DataTable title="Lịch sử biến động kho" emptyText="Không có dữ liệu phù hợp với bộ lọc." headers={historyColumns} {...tableProps}>{transactionRows(rows)}</DataTable>}

    {receipt && <WarehouseReceiptEditor key={`${receipt.mode}-${receipt.id || "new"}`} storeId={store.id} storeName={store.name} kind={tab} receiptId={receipt.id} readOnly={receipt.mode === "view"} onClose={closeReceipt} onChanged={refresh} />}
    {stock && <StockDetail row={stock} storeId={store.id} onClose={() => setStock(null)} />}
  </>;
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
  const linkedReceiptId = params.get("receipt");
  const clearLinkedReceipt = () => { const next = new URLSearchParams(params); next.delete("receipt"); setParams(next, { replace: true }); };
  const selectStore = id => { const next = new URLSearchParams(params); next.delete("receipt"); if (id) next.set("storeId", id); else next.delete("storeId"); setParams(next); };
  const setTab = id => { const next = new URLSearchParams(params); next.delete("receipt"); next.set("tab", id); if (storeId) next.set("storeId", storeId); setParams(next); };
  const storeSelect = <StoreSelector stores={stores} value={store?.id || ""} onChange={selectStore} required readOnly={isStore} label="Cửa hàng" placeholder="Chọn một cửa hàng"
    className={`${control} min-w-[240px] ${isGlobal ? "" : "opacity-80"}`} />;
  const pageVariant = tab === "import" ? "imports" : tab === "export" ? "exports" : "warehouse";
  return <div className={`admin-catalog-page admin-catalog-page--${pageVariant}`}>
    <AdminCatalogPageHeader icon={Warehouse} eyebrow="Vận hành kho" title={store ? `Kho · ${store.name}` : "Quản lý kho theo cửa hàng"} description="Theo dõi tồn Offline / Online, nhập xuất hàng và lịch sử biến động của từng cửa hàng." status={isStore ? currentStoreName : "Chọn cửa hàng để làm việc"} />
    {loading ? <p role="status" className="py-10 text-center text-zinc-400">Đang tải cửa hàng...</p>
      : error ? <p role="alert" className="rounded-xl border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}<Button onClick={reload} className="ml-3 text-amber-400 underline" variant="unstyled">Thử lại</Button></p>
        : store && tab ? <StoreWorkspace key={`${store.id}:${tab}`} store={store} storeSelect={storeSelect} tab={tab} setTab={setTab} allowedTabs={allowedTabs} linkedReceiptId={linkedReceiptId} clearLinkedReceipt={clearLinkedReceipt} />
          : <>
            <div className="admin-catalog-toolbar bg-zinc-900 border border-zinc-800 rounded-3xl p-6"><div className="flex flex-wrap items-end gap-4">{storeSelect}</div></div>
            <div className="admin-catalog-table bg-zinc-900 rounded-3xl overflow-hidden border border-zinc-800">
              <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-950"><h3 className="font-semibold text-lg">{stores.length ? "Chọn cửa hàng để xem và vận hành kho" : "Không có cửa hàng khả dụng"}</h3></div>
              <div className="p-6">
                {isGlobal && <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{stores.map(s => <button type="button" key={s.id} className="rounded-2xl border border-zinc-700 p-5 text-left hover:border-amber-400" onClick={() => selectStore(s.id)}><Warehouse className="mb-3 text-amber-300" size={22} /><span className="font-medium">{s.name}</span><p className="mt-1 text-sm text-zinc-400">Mở kho cửa hàng →</p></button>)}</div>}
                {storeId && !store && <p className="text-amber-300">Cửa hàng không còn hoạt động hoặc không thuộc phạm vi truy cập.</p>}
              </div>
            </div>
          </>}
  </div>;
}
