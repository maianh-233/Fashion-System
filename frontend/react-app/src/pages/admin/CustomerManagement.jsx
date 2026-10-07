import { useEffect, useState } from "react";
import { Eye, Pencil, Plus, RotateCcw, Search, Users } from "lucide-react";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import CustomerManagementDialog from "../../components/admin/Customer/CustomerManagementDialog";
import Button from "../../components/common/Button";
import Pagination from "../../components/common/Pagination";
import StoreSelector, { ALL_STORES } from "../../components/common/StoreSelector";
import { selectionToStoreFilter } from "../../components/common/storeSelectorLogic";
import { customerApi } from "../../api/adminManagementApi";
import { useAdminPermissions } from "../../contexts/AdminPermissionsContext";
import { useSystemNotification } from "../../components/common/SystemNotification";
import useStoreOptions from "../../hooks/useStoreOptions";

const control = "rounded-xl border border-zinc-700 bg-zinc-800 px-3 py-2 text-zinc-100";
const initialFilters = { search: "", store: ALL_STORES, source: "", tier: "", web: "", status: "" };

export default function CustomerManagement() {
  const { isGlobal, isStore, currentStoreId, currentStoreName, hasPermission } = useAdminPermissions();
  const notification = useSystemNotification();
  const { stores } = useStoreOptions({ endpoint: "/api/customers/store-options" });
  const [filters, setFilters] = useState(initialFilters);
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [page, setPage] = useState(1);
  const [result, setResult] = useState({ content: [], totalElements: 0, totalPages: 1 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
  const [dialog, setDialog] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => { const timer = setTimeout(() => setDebouncedSearch(filters.search.trim()), 300); return () => clearTimeout(timer); }, [filters.search]);
  useEffect(() => {
    const controller = new AbortController();
    const timer = setTimeout(() => {
      setLoading(true); setError("");
      const storeFilter = isGlobal ? selectionToStoreFilter(filters.store) : {};
      customerApi.list({ page: page - 1, size: 10, sort: "createdAt,desc", search: debouncedSearch,
        ...storeFilter, source: filters.source, tier: filters.tier,
        hasWebAccount: filters.web === "" ? undefined : filters.web === "true",
        active: filters.status === "" ? undefined : filters.status === "active" }, { signal: controller.signal })
        .then(setResult)
        .catch(requestError => { if (!controller.signal.aborted) setError(requestError.message || "Không thể tải khách hàng."); })
        .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    }, revision ? 0 : 0);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [debouncedSearch, filters.source, filters.status, filters.store, filters.tier, filters.web, isGlobal, page, revision]);

  const updateFilter = key => event => { setFilters(current => ({ ...current, [key]: event.target.value })); setPage(1); };
  const open = async (mode, row) => {
    if (mode === "create") { setDialog({ mode, customer: null }); return; }
    try { setDialog({ mode, customer: await customerApi.detail(row.id) }); }
    catch (requestError) { notification.error(requestError); }
  };
  const save = async body => {
    setSaving(true);
    try {
      if (dialog.mode === "create") await customerApi.create(body); else await customerApi.update(dialog.customer.id, body);
      notification.success(dialog.mode === "create" ? "Đã tạo Store Member." : "Đã cập nhật khách hàng.");
      setDialog(null); setRevision(value => value + 1);
    } catch (requestError) { notification.error(requestError); } finally { setSaving(false); }
  };
  const toggleActive = async row => {
    const accepted = await notification.confirm({ title: row.active ? "Ngưng khách hàng" : "Kích hoạt khách hàng", message: `${row.fullName} (${row.customerCode})?`, destructive: row.active });
    if (!accepted) return;
    try { await customerApi.setActive(row.id, !row.active); notification.success("Đã cập nhật trạng thái khách hàng."); setRevision(value => value + 1); }
    catch (requestError) { notification.error(requestError); }
  };
  const reset = () => { setFilters({ ...initialFilters, store: isStore ? currentStoreId : ALL_STORES }); setPage(1); };

  return <div className="admin-catalog-page admin-catalog-page--customer space-y-6 text-zinc-100">
    <AdminCatalogPageHeader icon={Users} eyebrow="Quan hệ khách hàng" title="Quản lý khách hàng" description={isGlobal ? "Khách hàng toàn hệ thống và website." : `Khách hàng thuộc ${currentStoreName || "cửa hàng hiện tại"}.`} />
    <section className="flex flex-wrap gap-3 rounded-3xl border border-zinc-800 bg-zinc-900 p-5">
      <label className="relative min-w-[260px] flex-1"><Search className="absolute left-3 top-2.5 text-zinc-500" size={18}/><input aria-label="Tìm khách hàng" className={`${control} w-full pl-10`} placeholder="Mã, tên, điện thoại, email…" value={filters.search} onChange={updateFilter("search")}/></label>
      <StoreSelector stores={stores} value={isStore ? currentStoreId || "" : filters.store} onChange={value => { setFilters(current => ({ ...current, store: value })); setPage(1); }} includeAll includeNoStore readOnly={isStore} label="" className={control}/>
      <Select label="Nguồn" value={filters.source} onChange={updateFilter("source")} options={[["STORE","Cửa hàng"],["WEBSITE","Website"]]}/>
      <Select label="Hạng" value={filters.tier} onChange={updateFilter("tier")} options={[["REGULAR","Regular"],["SILVER","Silver"],["GOLD","Gold"],["PLATINUM","Platinum"]]}/>
      <Select label="Web Account" value={filters.web} onChange={updateFilter("web")} options={[["true","Đã liên kết"],["false","Chưa liên kết"]]}/>
      <Select label="Trạng thái" value={filters.status} onChange={updateFilter("status")} options={[["active","Hoạt động"],["inactive","Ngưng hoạt động"]]}/>
      <Button onClick={reset} className={control}><RotateCcw size={17}/></Button>
      <Button permission="CUSTOMER_CREATE" onClick={() => open("create")} className="flex items-center gap-2 rounded-xl bg-amber-500 px-4 py-2 font-semibold text-zinc-950"><Plus size={17}/>Thêm khách hàng</Button>
    </section>
    {error && <p role="alert" className="rounded-xl bg-rose-500/10 p-3 text-rose-300">{error}</p>}
    <CustomerTable result={result} loading={loading} open={open} toggleActive={toggleActive} hasPermission={hasPermission}/>
    <Pagination currentPage={page} totalPages={Math.max(1, result.totalPages || 1)} onPageChange={setPage}/>
    {dialog && <CustomerManagementDialog {...dialog} stores={stores} isGlobal={isGlobal} currentStoreId={currentStoreId} busy={saving} onClose={() => !saving && setDialog(null)} onSave={save}/>}
  </div>;
}

function CustomerTable({ result, loading, open, toggleActive, hasPermission }) {
  const headers = ["Khách hàng","Điện thoại","Email","Nguồn","Cửa hàng nguồn","Hạng","Web","Trạng thái","Thao tác"];
  return <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900"><div className="flex justify-between border-b border-zinc-800 bg-zinc-950 p-5"><h2 className="font-semibold">Danh sách khách hàng</h2><span className="text-sm text-zinc-400">{result.totalElements} kết quả</span></div><div className="overflow-x-auto"><table className="w-full min-w-[1050px] text-left text-sm"><thead className="text-zinc-400"><tr>{headers.map(value => <th key={value} className="px-4 py-3 font-normal">{value}</th>)}</tr></thead><tbody className="divide-y divide-zinc-800">{loading ? <tr><td colSpan="9" className="p-10 text-center text-zinc-400">Đang tải…</td></tr> : result.content.length === 0 ? <tr><td colSpan="9" className="p-10 text-center text-zinc-400">Chưa có khách hàng phù hợp.</td></tr> : result.content.map(row => <tr key={row.id} className="hover:bg-zinc-800/60"><td className="px-4 py-4"><strong>{row.fullName}</strong><p className="text-xs text-amber-300">{row.customerCode}</p></td><td className="px-4 py-4">{row.phone || "—"}</td><td className="px-4 py-4">{row.email || "—"}</td><td className="px-4 py-4">{row.source}</td><td className="px-4 py-4">{row.originStoreName || "Website / —"}</td><td className="px-4 py-4">{row.tier || "REGULAR"}</td><td className="px-4 py-4">{row.hasWebAccount ? "Đã liên kết" : "Chưa có"}</td><td className="px-4 py-4"><span className={row.active ? "text-emerald-300" : "text-zinc-500"}>{row.active ? "Hoạt động" : "Ngưng"}</span></td><td className="px-4 py-4"><div className="flex gap-3"><Button title="Xem" onClick={() => open("view", row)} className="text-blue-300"><Eye size={17}/></Button>{hasPermission("CUSTOMER_UPDATE") && <Button title="Sửa" onClick={() => open("edit", row)} className="text-amber-300"><Pencil size={17}/></Button>}{hasPermission("CUSTOMER_STATUS_MANAGE") && <Button title="Đổi trạng thái" onClick={() => toggleActive(row)} className="text-zinc-300">{row.active ? "Ngưng" : "Bật"}</Button>}</div></td></tr>)}</tbody></table></div></section>;
}
function Select({ label, options, ...props }) { return <select aria-label={label} className={control} {...props}><option value="">Tất cả {label.toLowerCase()}</option>{options.map(([value, text]) => <option key={value} value={value}>{text}</option>)}</select>; }
