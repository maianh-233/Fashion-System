import Button from "../../components/common/Button";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Search, Plus, Eye, Settings, Trash2, RotateCcw, Lock, Unlock, Users, CircleCheck, UserPlus, Store } from "lucide-react";
import Pagination from "../../components/common/Pagination";
import EmployeeDialog from "../../components/admin/Empolyee/EmployeeDialog";
import CreatedEmployeeDialog from "../../components/admin/Empolyee/CreatedEmployeeDialog";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import { departmentApi, employeeApi, positionApi } from "../../hooks/adminManagementApi";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import { useAdminPermissions } from "../../contexts/AdminPermissionsContext";
import { useSystemNotification } from "../../components/common/SystemNotification";
import { refreshEmployeeData, updateEmployeeWithConfirmation } from "./positionManagementLogic";

const PAGE_SIZE = 10;
const ROLE_LABELS = { SUPER_ADMIN: "Quản trị viên tối cao", ADMIN: "Quản trị viên", MANAGER: "Quản lý", STAFF: "Nhân viên bán hàng", WAREHOUSE: "Nhân viên kho", ACCOUNTANT: "Kế toán" };

export default function EmployeeManagement() {
  const { user } = useAdminAuth();
  const { hasPermission } = useAdminPermissions();
  const notification = useSystemNotification();
  const roleCodes = useMemo(() => (user?.roles || []).map((role) => typeof role === "string" ? role : role.code), [user]);
  const canCreateAdmin = hasPermission("USER_CREATE_ADMIN");
  const superAdmin = roleCodes.includes("SUPER_ADMIN");
  const [filters, setFilters] = useState({ keyword: "", roleCode: "", status: "", storeId: "" });
  const [page, setPage] = useState(1);
  const [result, setResult] = useState({ content: [], totalElements: 0, totalPages: 1 });
  const [summary, setSummary] = useState({ total: 0, active: 0, locked: 0, newThisMonth: 0 });
  const [stores, setStores] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [positions, setPositions] = useState([]);
  const [scopeContext, setScopeContext] = useState(null);
  const [loading, setLoading] = useState(true);
  const [dialog, setDialog] = useState(null);
  const [saving, setSaving] = useState(false);
  const [createdEmployee, setCreatedEmployee] = useState(null);

  const load = useCallback(async (signal) => {
    setLoading(true);
    try {
      const params = { ...filters, page: page - 1, size: PAGE_SIZE, sort: "fullName,asc" };
      const [employees, stats] = await Promise.all([employeeApi.list(params, { signal }), employeeApi.summary(filters.storeId, { signal })]);
      setResult(employees); setSummary(stats);
    } catch (error) { if (error.name !== "AbortError") notification.error(error.message); }
    finally { if (!signal?.aborted) setLoading(false); }
  }, [filters, page, notification]);

  useEffect(() => {
    const controller = new AbortController();
    Promise.all([
      employeeApi.scope({ signal: controller.signal }),
      employeeApi.availableStores({ signal: controller.signal }),
      departmentApi.list({ active: true, page: 0, size: 500, sort: "name,asc" }, { signal: controller.signal }),
      positionApi.list({ active: true, page: 0, size: 500, sort: "name,asc" }, { signal: controller.signal }),
    ]).then(([scopeData, storeData, departmentData, positionData]) => {
      setScopeContext(scopeData);
      setFilters((current) => scopeData.scope === "STORE" ? { ...current, storeId: "" } : current);
      setStores(storeData); setDepartments(departmentData.content || []); setPositions(positionData.content || []);
    }).catch((error) => { if (error.name !== "AbortError") notification.error(error.message); });
    return () => controller.abort();
  }, [notification]);
  useEffect(() => { const controller = new AbortController(); Promise.resolve().then(() => load(controller.signal)); return () => controller.abort(); }, [load]);

  const updateFilter = (key) => (event) => { setPage(1); setFilters((current) => ({ ...current, [key]: event.target.value })); };
  const openEmployee = async (mode, employee) => {
    try { setDialog({ mode, employee: await employeeApi.detail(employee.id) }); }
    catch (error) { notification.error(error.message); }
  };
  const refresh = async (text) => { setDialog(null); notification.success(text); await load(); };
  const reloadCatalogs = async () => {
    const [departmentData, positionData] = await Promise.all([
      departmentApi.list({ active: true, page: 0, size: 500, sort: "name,asc" }),
      positionApi.list({ active: true, page: 0, size: 500, sort: "name,asc" }),
    ]);
    return { departments: departmentData.content || [], positions: positionData.content || [] };
  };
  const save = async (data) => {
    if (saving) return;
    setSaving(true);
    try {
      if (dialog.mode === "create") {
        const result = await employeeApi.create(data);
        setDialog(null);
        setCreatedEmployee(result);
        notification.success("Đã tạo nhân viên và ghi nhật ký hệ thống.");
        await load();
      } else {
        const saved = await updateEmployeeWithConfirmation({ original: dialog.employee, form: data, api: employeeApi, confirm: notification.confirm });
        if (!saved) return;
        // Close the stale edit form even if a later refresh fails: the write succeeded.
        const employeeId = dialog.employee.id;
        setDialog(null);
        notification.success("Đã cập nhật hồ sơ nhân viên.");
        const fresh = await refreshEmployeeData({ id: employeeId, api: employeeApi, reloadEmployees: load, reloadCatalogs });
        setDepartments(fresh.catalogs.departments);
        setPositions(fresh.catalogs.positions);
        setDialog({ mode: "view", employee: fresh.employee, subordinateData: fresh.subordinateData });
      }
    } catch (error) { notification.error(error.message); } finally { setSaving(false); }
  };
  const action = async (callback, text) => { try { await callback(); await refresh(text); } catch (error) { notification.error(error.message); } };
  const stats = [["Tổng nhân viên", summary.total, Users, "text-blue-400"], ["Đang hoạt động", summary.active, CircleCheck, "text-emerald-400"], ["Bị khóa", summary.locked, Lock, "text-red-400"], ["Mới trong tháng", summary.newThisMonth, UserPlus, "text-amber-400"]];

  return <div className="admin-catalog-page admin-catalog-page--employees">
    <AdminCatalogPageHeader icon={Users} eyebrow="Quản trị nhân sự" title="Quản lý nhân viên" description={scopeContext?.scope === "ALL" ? "Bạn đang xem nhân sự trên toàn hệ thống." : scopeContext?.storeName ? `Store: ${scopeContext.storeName}` : "Đang xác định phạm vi dữ liệu nhân viên."} />
    {scopeContext?.scope === "STORE" && <div className="mb-5 inline-flex items-center gap-2 rounded-full border border-amber-500/30 bg-amber-500/10 px-4 py-2 text-sm text-amber-300"><Store size={15}/>Store scope · {scopeContext.storeName}</div>}
    <div className="admin-catalog-toolbar mb-8 rounded-3xl border border-zinc-800 bg-zinc-900 p-6"><div className="flex flex-wrap items-center gap-3">
      <div className="relative min-w-[16rem] flex-1"><input value={filters.keyword} onChange={updateFilter("keyword")} placeholder="Tên, email, SĐT hoặc mã nhân viên..." className="w-full rounded-2xl border border-zinc-700 bg-zinc-800 py-3 pl-11 pr-4 text-sm outline-none focus:border-amber-400"/><Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500"/></div>
      <select value={filters.roleCode} onChange={updateFilter("roleCode")} className="rounded-2xl border border-zinc-700 bg-zinc-800 px-4 py-3 text-sm"><option value="">Tất cả vai trò</option>{Object.entries(ROLE_LABELS).map(([code,label])=><option key={code} value={code}>{label}</option>)}</select>
      <select value={filters.status} onChange={updateFilter("status")} className="rounded-2xl border border-zinc-700 bg-zinc-800 px-4 py-3 text-sm"><option value="">Tất cả trạng thái</option><option value="ACTIVE">Hoạt động</option><option value="INACTIVE">Ngừng hoạt động</option><option value="DELETED">Đã xóa</option></select>
      {scopeContext?.scope === "ALL" && stores.length > 0 && <select value={filters.storeId} onChange={updateFilter("storeId")} className="rounded-2xl border border-zinc-700 bg-zinc-800 px-4 py-3 text-sm"><option value="">Tất cả cửa hàng</option>{stores.map((store)=><option key={store.id} value={store.id}>{store.name}</option>)}</select>}
      <Button onClick={()=>{setPage(1);setFilters({keyword:"",roleCode:"",status:"",storeId:""});}} className="flex items-center gap-2 rounded-2xl bg-blue-500 px-5 py-3"><RotateCcw size={17}/>Reset</Button>
      <Button permission="USER_CREATE" onClick={()=>setDialog({mode:"create",employee:null})} className="flex items-center gap-2 rounded-2xl bg-amber-500 px-5 py-3 font-semibold text-zinc-950"><Plus size={17}/>Thêm nhân viên</Button>
    </div></div>
    <div className="admin-catalog-stats mb-8 grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">{stats.map(([label,value,Icon,color])=><div key={label} className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6"><div className="flex justify-between"><div><p className="text-zinc-400">{label}</p><p className={`mt-2 text-4xl font-bold ${color}`}>{value}</p></div><Icon size={38} className={color}/></div></div>)}</div>
    <div className="admin-catalog-table overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900"><div className="flex items-center justify-between border-b border-zinc-800 bg-zinc-950 p-6"><h3 className="text-lg font-semibold">Danh sách nhân viên</h3><span className="text-sm text-zinc-400">{result.totalElements} kết quả</span></div>
      <div className="overflow-x-auto"><table className="w-full"><thead><tr className="border-b border-zinc-800 text-sm text-zinc-400"><th className="px-6 py-4 text-left font-normal">Nhân viên</th><th className="px-6 py-4 text-left font-normal">Liên hệ</th><th className="px-6 py-4 text-left font-normal">Cửa hàng</th><th className="px-6 py-4 text-center font-normal">Vai trò</th><th className="px-6 py-4 text-center font-normal">Trạng thái</th><th className="px-6 py-4 text-center font-normal">Thao tác</th></tr></thead>
      <tbody className="divide-y divide-zinc-800 text-sm">{loading ? <tr><td colSpan="6" className="px-6 py-12 text-center text-zinc-500">Đang tải dữ liệu...</td></tr> : result.content.length===0 ? <tr><td colSpan="6" className="px-6 py-12 text-center text-zinc-500">Không tìm thấy nhân viên phù hợp.</td></tr> : result.content.map((employee)=><EmployeeRow key={employee.id} employee={employee} openEmployee={openEmployee} action={action} />)}</tbody></table></div>
      <Pagination currentPage={page} totalPages={Math.max(1,result.totalPages||1)} onPageChange={setPage}/>
    </div>
    {dialog && <EmployeeDialog key={`${dialog.mode}-${dialog.employee?.id || "new"}`} {...dialog} stores={stores} departments={departments} positions={positions} scopeContext={scopeContext} privileged={canCreateAdmin} superAdmin={superAdmin} busy={saving} onClose={()=>{if (!saving) setDialog(null);}} onSave={save}/>}
    {createdEmployee && <CreatedEmployeeDialog result={createdEmployee} onClose={()=>setCreatedEmployee(null)} />}
  </div>;
}

function EmployeeRow({ employee, openEmployee, action }) {
  const notification = useSystemNotification();
  const remove = async () => {
    if (!await notification.confirm({ title: "Xóa mềm nhân viên", message: `Xóa mềm nhân viên ${employee.fullName}? Bạn có thể khôi phục hồ sơ sau đó.`, confirmText: "Xóa mềm", cancelText: "Hủy", destructive: true })) return;
    await action(() => employeeApi.remove(employee.id), "Đã xóa mềm nhân viên.");
  };
  return <tr className="hover:bg-zinc-800/60">
    <td className="px-6 py-5"><div className="flex items-center gap-3"><span className="grid h-10 w-10 place-items-center rounded-xl bg-amber-500/15 font-bold text-amber-400">{initials(employee.fullName)}</span><div><strong>{employee.fullName}</strong><p className="mt-1 text-xs text-zinc-500">{employee.employeeCode} · @{employee.username}</p></div></div></td>
    <td className="px-6 py-5 text-zinc-300"><p>{employee.email}</p><p className="mt-1 text-xs text-zinc-500">{employee.phone || "Chưa có SĐT"}</p></td>
    <td className="px-6 py-5"><span className="inline-flex items-center gap-2"><Store size={15} className="text-amber-400"/>{employee.storeName || "Chưa phân công"}</span></td>
    <td className="px-6 py-5 text-center"><span className="rounded-full bg-zinc-700 px-3 py-1 text-xs">{employee.roleCodes.map((role)=>ROLE_LABELS[role]||role).join(", ")}</span></td>
    <td className="px-6 py-5 text-center">{statusBadge(employee)}</td>
    <td className="px-6 py-5"><div className="flex justify-center gap-3">
      <Button title="Xem" onClick={()=>openEmployee("view", employee)} className="text-blue-400"><Eye size={18}/></Button>
      <Button permission="USER_UPDATE" title="Sửa" onClick={()=>openEmployee("edit", employee)} className="text-amber-400"><Settings size={18}/></Button>
      {!employee.deletedAt && <Button permission="USER_UPDATE" title={employee.locked?"Mở khóa":"Khóa"} onClick={()=>action(()=>employeeApi.setLocked(employee.id,!employee.locked),employee.locked?"Đã mở khóa tài khoản.":"Đã khóa tài khoản.")} className={employee.locked?"text-emerald-400":"text-orange-400"}>{employee.locked?<Unlock size={18}/>:<Lock size={18}/>}</Button>}
      {employee.deletedAt ? <Button permission="USER_UPDATE" title="Khôi phục" onClick={()=>action(()=>employeeApi.restore(employee.id),"Đã khôi phục nhân viên.")} className="text-emerald-400"><RotateCcw size={18}/></Button> : <Button permission="USER_DELETE" title="Xóa mềm" onClick={remove} className="text-red-400"><Trash2 size={18}/></Button>}
    </div></td>
  </tr>;
}
function initials(name="") { return name.trim().split(/\s+/).slice(-2).map((part)=>part[0]).join("").toUpperCase(); }
function statusBadge(employee) { if(employee.deletedAt)return <span className="rounded-full bg-red-500/15 px-3 py-1 text-xs text-red-400">Đã xóa</span>; if(employee.locked)return <span className="rounded-full bg-orange-500/15 px-3 py-1 text-xs text-orange-400">Đã khóa</span>; if(employee.active)return <span className="rounded-full bg-emerald-500/15 px-3 py-1 text-xs text-emerald-400">Hoạt động</span>; return <span className="rounded-full bg-zinc-700 px-3 py-1 text-xs text-zinc-300">Ngừng hoạt động</span>; }
