import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  AlertTriangle, ArrowDown, ArrowUp, ArrowUpDown, CircleCheck, DollarSign, Eye, EyeOff, Plus, RotateCcw,
  Search, ShoppingBag, Store,
} from "lucide-react";
import Button from "../../components/common/Button";
import Pagination from "../../components/common/Pagination";
import StoreSelector from "../../components/common/StoreSelector";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import OrderDetailDialog from "../../components/admin/Order/OrderDetailDialog";
import { useSystemNotification } from "../../components/common/SystemNotification";
import { useAdminPermissions } from "../../contexts/AdminPermissionsContext";
import useStoreOptions from "../../hooks/useStoreOptions";
import { orderApi } from "../../api/admin/orderApi";
import { useRealtime } from "../../realtime/RealtimeContext";
import { money } from "./offlinePosLogic";
import {
  EMPTY_FILTERS, ORDER_STATUS_LABELS, ORDER_STATUS_STYLES, PAYMENT_METHOD_LABELS, PAYMENT_STATUS_LABELS,
  customerLabel, listParams, nextSort, statusOptions, validateDates,
} from "./orderManagementLogic";

const PAGE_SIZE = 10;
const control = "bg-zinc-800 border border-zinc-700 rounded-2xl py-3 px-4 text-sm";
const TYPES = [["ONLINE", "Đơn online"], ["OFFLINE", "Đơn tại quầy"]];
const FINISHED = ["COMPLETED", "CANCELLED", "RETURNED"];
const dateTime = value => value ? new Date(value).toLocaleString("vi-VN") : "—";

function SortHeader({ label, field, sort, onSort, align = "left" }) {
  const Icon = sort.field !== field ? ArrowUpDown : sort.direction === "desc" ? ArrowDown : ArrowUp;
  return <th className={`px-6 py-5 font-normal ${align === "right" ? "text-right" : "text-left"}`}>
    <button type="button" className={`inline-flex items-center gap-1 ${sort.field === field ? "text-amber-400" : ""}`} onClick={() => onSort(field)}>
      {label} <Icon size={14} />
    </button>
  </th>;
}

/**
 * Quản trị đơn hàng theo cửa hàng: nhân viên Global chọn cửa hàng, nhân viên Store chỉ thấy cửa hàng mình.
 * Lọc, sắp xếp và phân trang đều chạy ở backend.
 */
export default function OrderManagement() {
  const navigate = useNavigate();
  const notification = useSystemNotification();
  const [searchParams, setSearchParams] = useSearchParams();
  const { isGlobal, currentStoreId, currentStoreName, hasPermission, loading: permissionsLoading } = useAdminPermissions();
  const { stores, loading: storesLoading, error: storesError } = useStoreOptions({ endpoint: "/api/admin/orders/stores", enabled: !permissionsLoading });
  const [storeId, setStoreId] = useState("");
  const [type, setType] = useState("ONLINE");
  const [hidden, setHidden] = useState(false);
  const [draft, setDraft] = useState(EMPTY_FILTERS);
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [sort, setSort] = useState({ field: "createdAt", direction: "desc" });
  const [page, setPage] = useState(1);
  const [revision, setRevision] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [summary, setSummary] = useState(null);
  const [handlers, setHandlers] = useState([]);
  const [busyId, setBusyId] = useState("");
  const { subscribe } = useRealtime();

  const effectiveStoreId = isGlobal ? storeId : currentStoreId || stores[0]?.id || "";
  const storeName = isGlobal ? stores.find(s => s.id === storeId)?.name : currentStoreName || stores[0]?.name;
  // Link từ thông báo/trang khác (?order=<id>) mở thẳng chi tiết đơn.
  const openedOrderId = searchParams.get("order");
  const openOrder = id => setSearchParams(current => { const next = new URLSearchParams(current); next.set("order", id); return next; });
  const closeOrder = () => setSearchParams(current => { const next = new URLSearchParams(current); next.delete("order"); return next; });

  useEffect(() => {
    if (!effectiveStoreId) return undefined;
    const controller = new AbortController();
    const signal = controller.signal;
    orderApi.list(listParams({ storeId: effectiveStoreId, type, hidden, filters, page, size: PAGE_SIZE, sort }), { signal })
      .then(result => { setData(result); setError(""); })
      .catch(failure => { if (!signal.aborted) setError(failure.message); })
      .finally(() => { if (!signal.aborted) setLoading(false); });
    orderApi.summary({ storeId: effectiveStoreId, type }, { signal })
      .then(setSummary).catch(() => { if (!signal.aborted) setSummary(null); });
    return () => controller.abort();
  }, [effectiveStoreId, type, hidden, filters, page, sort, revision]);

  useEffect(() => {
    if (!effectiveStoreId) return undefined;
    const controller = new AbortController();
    orderApi.handlers(effectiveStoreId, { signal: controller.signal })
      .then(setHandlers).catch(() => { if (!controller.signal.aborted) setHandlers([]); });
    return () => controller.abort();
  }, [effectiveStoreId]);

  const reload = () => { setLoading(true); setRevision(value => value + 1); };

  // Có thông báo về đơn (đơn mới, khách hủy...) thì tải lại danh sách để nhân viên thấy ngay.
  useEffect(() => subscribe({ channel: "NOTIFICATION", eventType: "NOTIFICATION_CREATED" }, event => {
    if (event?.payload?.entityType === "ORDER") setRevision(value => value + 1);
  }), [subscribe]);
  const changeView = update => { setLoading(true); setPage(1); update(); };
  const dateError = validateDates(draft);
  const applyFilters = event => {
    event.preventDefault();
    if (dateError) return;
    changeView(() => setFilters({ ...draft }));
  };
  const resetFilters = () => changeView(() => { setDraft(EMPTY_FILTERS); setFilters(EMPTY_FILTERS); });
  const changeType = value => changeView(() => {
    setType(value);
    setDraft(current => ({ ...current, status: "" }));
    setFilters(current => ({ ...current, status: "" }));
  });

  const toggleVisibility = async row => {
    const hide = row.active;
    const confirmed = await notification.confirm(hide
      ? `Ẩn đơn ${row.orderCode} khỏi danh sách? Dữ liệu không bị xóa và có thể khôi phục.`
      : `Khôi phục đơn ${row.orderCode} về danh sách?`, { confirmText: hide ? "Ẩn đơn" : "Khôi phục" });
    if (!confirmed) return;
    setBusyId(row.id);
    try {
      await (hide ? orderApi.hide(row.id) : orderApi.restore(row.id));
      notification.success(hide ? `Đã ẩn đơn ${row.orderCode}.` : `Đã khôi phục đơn ${row.orderCode}.`);
      reload();
    } catch (failure) {
      notification.error(failure.message);
    } finally {
      setBusyId("");
    }
  };

  const counts = summary?.statusCounts || {};
  const needsAction = (counts.PENDING || 0) + (counts.CONFIRMED || 0) + (counts.DELIVERY_FAILED || 0);
  const cards = [
    { key: "today", label: "Đơn hôm nay", value: summary?.todayCount, icon: ShoppingBag, className: "text-blue-400" },
    { key: "revenue", label: "Doanh thu hôm nay", value: summary ? money(summary.todayRevenue) : undefined, icon: DollarSign, className: "text-amber-400" },
    type === "ONLINE"
      ? { key: "todo", label: "Cần xử lý", value: summary ? needsAction : undefined, icon: AlertTriangle, className: "text-red-400" }
      : { key: "done", label: "Hoàn thành", value: counts.COMPLETED, icon: CircleCheck, className: "text-emerald-400" },
    { key: "hidden", label: "Đơn đã ẩn", value: summary?.hiddenCount, icon: EyeOff, className: "text-zinc-400" },
  ];
  const rows = data?.content || [];
  const colSpan = 8;
  const sortBy = field => changeView(() => setSort(current => nextSort(current, field)));

  return <div className="admin-catalog-page admin-catalog-page--orders text-zinc-200">
    <AdminCatalogPageHeader icon={ShoppingBag} eyebrow="Vận hành bán hàng" title={storeName ? `Đơn hàng · ${storeName}` : "Quản lý đơn hàng"}
      description="Theo dõi đơn online và đơn tại quầy, trạng thái xử lý, thanh toán và lịch sử của từng đơn."
      action={<Button permission="ORDER_CREATE" variant="primary" onClick={() => navigate("/admin/orders/pos")}><Plus size={16} /> Bán tại quầy</Button>} />

    <div className="admin-catalog-toolbar bg-zinc-900 border border-zinc-800 rounded-3xl p-6 space-y-4">
      <div className="flex flex-wrap items-center gap-4">
        {isGlobal
          ? <StoreSelector stores={stores} value={storeId} onChange={value => changeView(() => setStoreId(value))} required loading={storesLoading} label="Cửa hàng" placeholder="Chọn cửa hàng" className={control} />
          : <span className="inline-flex items-center gap-2 text-sm text-zinc-400"><Store size={16} /> {storeName || "Cửa hàng của bạn"}</span>}
        {storesError && <span className="text-sm text-red-400">{storesError}</span>}
        <div className="flex flex-wrap gap-2" role="tablist" aria-label="Loại đơn">
          {TYPES.map(([value, label]) => <Button key={value} role="tab" aria-selected={type === value}
            variant={type === value ? "primary" : "secondary"} onClick={() => changeType(value)}>{label}</Button>)}
          <Button variant={hidden ? "primary" : "ghost"} aria-pressed={hidden} onClick={() => changeView(() => setHidden(value => !value))}>
            <EyeOff size={16} /> Đơn đã ẩn
          </Button>
        </div>
      </div>
      <form className="flex flex-wrap gap-3 items-center" onSubmit={applyFilters}>
        <div className="flex-1 min-w-[16rem] relative">
          <Search size={18} className="absolute left-4 top-3.5 text-zinc-500" />
          <input className={`${control} w-full pl-11`} placeholder="Mã đơn, tên hoặc SĐT khách" aria-label="Tìm đơn"
            value={draft.keyword} onChange={event => setDraft({ ...draft, keyword: event.target.value })} />
        </div>
        <select className={control} aria-label="Trạng thái" value={draft.status} onChange={event => setDraft({ ...draft, status: event.target.value })}>
          <option value="">Tất cả trạng thái</option>
          {statusOptions(type).map(code => <option key={code} value={code}>{ORDER_STATUS_LABELS[code]}</option>)}
        </select>
        <input type="date" className={control} aria-label="Từ ngày" value={draft.fromDate} onChange={event => setDraft({ ...draft, fromDate: event.target.value })} />
        <input type="date" className={control} aria-label="Đến ngày" value={draft.toDate} onChange={event => setDraft({ ...draft, toDate: event.target.value })} />
        <select className={control} aria-label="Nhân viên xử lý" value={draft.handlerId} onChange={event => setDraft({ ...draft, handlerId: event.target.value })}>
          <option value="">Mọi nhân viên</option>
          {handlers.map(h => <option key={h.id} value={h.id}>{h.name}</option>)}
        </select>
        <select className={control} aria-label="Phương thức thanh toán" value={draft.paymentMethod} onChange={event => setDraft({ ...draft, paymentMethod: event.target.value })}>
          <option value="">Mọi phương thức</option>
          {Object.entries(PAYMENT_METHOD_LABELS).map(([code, label]) => <option key={code} value={code}>{label}</option>)}
        </select>
        <Button className="inline-flex items-center gap-2" type="submit" variant="primary" disabled={!!dateError || !effectiveStoreId}><Search size={16} /> Lọc</Button>
        <Button className="inline-flex items-center gap-2" type="button" variant="ghost" onClick={resetFilters}><RotateCcw size={16} /> Đặt lại</Button>
        {dateError && <p className="w-full text-sm text-red-400">{dateError}</p>}
      </form>
    </div>

    {!effectiveStoreId ? <div className="admin-catalog-table bg-zinc-900 rounded-3xl border border-zinc-800 p-10 text-center text-zinc-400">
      {isGlobal ? "Chọn cửa hàng để xem đơn hàng." : "Đang tải cửa hàng…"}
    </div> : <>
      <div className="admin-catalog-stats grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-6">
        {cards.map(({ key, label, value, icon: Icon, className }) => <div key={key} className="bg-zinc-900 p-6 rounded-3xl border border-zinc-800">
          <div className="flex items-center justify-between gap-3">
            <div><p className="text-zinc-400 text-sm">{label}</p><p className={`text-3xl font-bold mt-1 ${className}`}>{value ?? "–"}</p></div>
            <Icon size={36} className={className} />
          </div>
        </div>)}
      </div>

      <div className="admin-catalog-table bg-zinc-900 rounded-3xl border border-zinc-800 overflow-hidden">
        <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-950">
          <h3 className="font-semibold text-lg">{hidden ? `Đơn đã ẩn · ${TYPES.find(([value]) => value === type)[1]}` : TYPES.find(([value]) => value === type)[1]}</h3>
          <p className="text-sm text-zinc-400">Tìm thấy:<span className="font-medium text-white ml-1">{(data?.totalElements ?? 0).toLocaleString("vi-VN")}</span></p>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead><tr className="border-b border-zinc-800 text-zinc-400 text-sm">
              <SortHeader label="Mã đơn / Thời gian" field="createdAt" sort={sort} onSort={sortBy} />
              <th className="px-6 py-5 font-normal text-left">Khách hàng</th>
              <th className="px-6 py-5 font-normal text-left">Trạng thái</th>
              <th className="px-6 py-5 font-normal text-left">Thanh toán</th>
              <th className="px-6 py-5 font-normal text-center">SL</th>
              <SortHeader label="Tổng tiền" field="totalAmount" sort={sort} align="right" onSort={sortBy} />
              <th className="px-6 py-5 font-normal text-left">Nhân viên xử lý</th>
              <th className="px-6 py-5 font-normal text-center">Thao tác</th>
            </tr></thead>
            <tbody className="divide-y divide-zinc-800 text-sm">
              {loading && <tr><td colSpan={colSpan} className="px-6 py-10 text-center text-zinc-400">Đang tải dữ liệu...</td></tr>}
              {!loading && error && <tr><td colSpan={colSpan} className="px-6 py-10 text-center text-red-400">{error}<Button onClick={reload} className="ml-3 text-amber-400 underline" variant="unstyled">Thử lại</Button></td></tr>}
              {!loading && !error && !rows.length && <tr><td colSpan={colSpan} className="px-6 py-10 text-center text-zinc-400">{hidden ? "Không có đơn đã ẩn." : "Không có đơn hàng phù hợp."}</td></tr>}
              {!loading && !error && rows.map(row => {
                const customer = customerLabel(row);
                // Nút nhanh chỉ để tiện thao tác; backend vẫn kiểm tra trạng thái và quyền.
                const canToggle = row.active ? hasPermission("ORDER_HIDE") && FINISHED.includes(row.status) : hasPermission("ORDER_RESTORE");
                return <tr key={row.id} className="hover:bg-zinc-800 transition-colors">
                  <td className="px-6 py-5"><span className="font-medium text-white">{row.orderCode}</span><span className="block text-xs text-zinc-400">{dateTime(row.createdAt)}</span></td>
                  <td className="px-6 py-5">{customer.name}{customer.phone && <span className="block text-xs text-zinc-400">{customer.phone}</span>}</td>
                  <td className="px-6 py-5"><span className={`px-4 py-1 rounded-full text-xs whitespace-nowrap ${ORDER_STATUS_STYLES[row.status] || "bg-zinc-700"}`}>{ORDER_STATUS_LABELS[row.status] || row.status}</span></td>
                  <td className="px-6 py-5">{PAYMENT_METHOD_LABELS[row.paymentMethod] || row.paymentMethod || "—"}<span className="block text-xs text-zinc-400">{PAYMENT_STATUS_LABELS[row.paymentStatus] || row.paymentStatus}</span></td>
                  <td className="px-6 py-5 text-center">{row.itemCount}</td>
                  <td className="px-6 py-5 text-right font-semibold text-white whitespace-nowrap">{money(row.totalAmount)}</td>
                  <td className="px-6 py-5">{row.handlerName || "—"}</td>
                  <td className="px-6 py-5">
                    <div className="flex items-center justify-center gap-2">
                      <Button variant="ghost" size="sm" aria-label={`Xem đơn ${row.orderCode}`} onClick={() => openOrder(row.id)}><Eye size={16} /></Button>
                      {canToggle && <Button variant={row.active ? "danger" : "ghost"} size="sm" loading={busyId === row.id}
                        aria-label={row.active ? `Ẩn đơn ${row.orderCode}` : `Khôi phục đơn ${row.orderCode}`} onClick={() => toggleVisibility(row)}>
                        {row.active ? <EyeOff size={16} /> : <RotateCcw size={16} />}
                      </Button>}
                    </div>
                  </td>
                </tr>;
              })}
            </tbody>
          </table>
        </div>
        <Pagination currentPage={page} totalPages={Math.max(1, data?.totalPages || 1)} onPageChange={value => { if (value !== page) changeView(() => setPage(value)); }} />
      </div>
    </>}

    {openedOrderId && <OrderDetailDialog key={openedOrderId} orderId={openedOrderId} onClose={closeOrder} onChanged={reload} />}
  </div>;
}
