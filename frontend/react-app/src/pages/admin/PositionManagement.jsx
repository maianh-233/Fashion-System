import { useCallback, useEffect, useState } from "react";
import { BriefcaseBusiness, CircleCheck, Eye, Pencil, Plus, RotateCcw, Search, Trash2 } from "lucide-react";
import Button from "../../components/common/Button";
import Pagination from "../../components/common/Pagination";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../components/admin/common/AdminDialog";
import { useSystemNotification } from "../../components/common/SystemNotification";
import { formatSalaryRange, positionErrorMessage, toCreatePositionPayload, updatePositionWithConfirmation, validatePositionForm } from "./positionManagementLogic";
import { departmentApi, positionApi } from "../../api/adminManagementApi";

const EMPTY = { departmentId: "", code: "", name: "", description: "", active: true, hierarchyLevel: "1", minSalary: "0", maxSalary: "0" };

export default function PositionManagement() {
  const notification = useSystemNotification();
  const [departments,setDepartments]=useState([]); const [filters,setFilters]=useState({keyword:"",departmentId:"",active:""}); const [page,setPage]=useState(1);
  const [result,setResult]=useState({content:[],totalElements:0,totalPages:1}); const [loading,setLoading]=useState(true); const [dialog,setDialog]=useState(null);
  const load=useCallback(async(signal)=>{setLoading(true);try{setResult(await positionApi.list({...filters,page:page-1,size:10,sort:"name,asc"},{signal}));}catch(error){if(error.name!=="AbortError")notification.error(positionErrorMessage(error));}finally{if(!signal?.aborted)setLoading(false);}},[filters,notification,page]);
  useEffect(()=>{const controller=new AbortController();departmentApi.list({active:true,page:0,size:500,sort:"name,asc"},{signal:controller.signal}).then((data)=>setDepartments(data.content||[])).catch((error)=>{if(error.name!=="AbortError")notification.error(positionErrorMessage(error));});return()=>controller.abort();},[notification]);
  useEffect(()=>{const controller=new AbortController();Promise.resolve().then(()=>load(controller.signal));return()=>controller.abort();},[load]);
  const refresh=async(text)=>{setDialog(null);notification.success(text);await load();};
  const remove=async(item)=>{if(!await notification.confirm({title:"Xác nhận xóa vị trí",message:`Xóa mềm vị trí ${item.name}?`,confirmText:"Xóa vị trí",cancelText:"Hủy",destructive:true}))return;try{await positionApi.remove(item.id);await refresh("Đã xóa mềm vị trí.");}catch(error){notification.error(positionErrorMessage(error));}};
  const restore=async(item)=>{try{await positionApi.restore(item.id);await refresh("Đã khôi phục vị trí.");}catch(error){notification.error(positionErrorMessage(error));}};
  return <div className="admin-catalog-page">
    <AdminCatalogPageHeader icon={BriefcaseBusiness} eyebrow="Cơ cấu tổ chức" title="Quản lý vị trí" description="Danh mục vị trí theo phòng ban, sẵn sàng dùng chung cho nhân sự và tuyển dụng."/>
    <div className="admin-catalog-toolbar mb-8 rounded-3xl border border-zinc-800 bg-zinc-900 p-6"><div className="flex flex-wrap gap-3"><div className="relative min-w-[16rem] flex-1"><input value={filters.keyword} onChange={(event)=>{setPage(1);setFilters((current)=>({...current,keyword:event.target.value}));}} placeholder="Tìm theo tên hoặc mã vị trí..." className="w-full rounded-2xl border border-zinc-700 bg-zinc-800 py-3 pl-11 pr-4 text-sm outline-none focus:border-amber-400"/><Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500"/></div><select value={filters.departmentId} onChange={(event)=>{setPage(1);setFilters((current)=>({...current,departmentId:event.target.value}));}} className="rounded-2xl border border-zinc-700 bg-zinc-800 px-4 py-3 text-sm"><option value="">Tất cả phòng ban</option>{departments.map((item)=><option key={item.id} value={item.id}>{item.name}</option>)}</select><select value={filters.active} onChange={(event)=>{setPage(1);setFilters((current)=>({...current,active:event.target.value}));}} className="rounded-2xl border border-zinc-700 bg-zinc-800 px-4 py-3 text-sm"><option value="">Tất cả trạng thái</option><option value="true">Hoạt động</option><option value="false">Ngừng hoạt động</option></select><Button onClick={()=>{setPage(1);setFilters({keyword:"",departmentId:"",active:""});}} className="flex items-center gap-2 rounded-2xl bg-blue-500 px-5 py-3"><RotateCcw size={17}/>Reset</Button><Button permission="POSITION_CREATE" onClick={()=>setDialog({mode:"create",item:null})} className="flex items-center gap-2 rounded-2xl bg-amber-500 px-5 py-3 font-semibold text-zinc-950"><Plus size={17}/>Thêm vị trí</Button></div></div>
    <div className="mb-8 grid grid-cols-1 gap-5 sm:grid-cols-2"><Stat label="Tổng vị trí" value={result.totalElements} icon={BriefcaseBusiness} color="text-blue-400"/><Stat label="Đang hoạt động trên trang" value={result.content.filter((item)=>item.active).length} icon={CircleCheck} color="text-emerald-400"/></div>
    <div className="admin-catalog-table overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900"><div className="border-b border-zinc-800 bg-zinc-950 p-6"><h3 className="text-lg font-semibold">Danh sách vị trí</h3></div><div className="overflow-x-auto"><table className="w-full"><thead><tr className="border-b border-zinc-800 text-sm text-zinc-400"><th className="px-6 py-4 text-left font-normal">Vị trí</th><th className="px-6 py-4 text-left font-normal">Phòng ban</th><th className="px-6 py-4 text-left font-normal">Cấp bậc</th><th className="px-6 py-4 text-left font-normal">Khoảng lương (VND)</th><th className="px-6 py-4 text-left font-normal">Mô tả</th><th className="px-6 py-4 text-center font-normal">Trạng thái</th><th className="px-6 py-4 text-center font-normal">Thao tác</th></tr></thead><tbody className="divide-y divide-zinc-800 text-sm">{loading?<tr><td colSpan="7" className="px-6 py-12 text-center text-zinc-500">Đang tải dữ liệu...</td></tr>:result.content.length===0?<tr><td colSpan="7" className="px-6 py-12 text-center text-zinc-500">Chưa có vị trí phù hợp.</td></tr>:result.content.map((item)=><tr key={item.id} className="hover:bg-zinc-800/60"><td className="px-6 py-5"><strong>{item.name}</strong><p className="mt-1 text-xs text-amber-400">{item.code}</p></td><td className="px-6 py-5 text-zinc-300">{item.departmentName}</td><td className="px-6 py-5">{item.hierarchyLevel}</td><td className="whitespace-nowrap px-6 py-5 text-zinc-300">{formatSalaryRange(item.minSalary,item.maxSalary)}</td><td className="max-w-md px-6 py-5 text-zinc-400">{item.description||"Chưa có mô tả"}</td><td className="px-6 py-5 text-center"><Badge active={item.active}/></td><td className="px-6 py-5"><div className="flex justify-center gap-3"><Button title="Xem" onClick={()=>setDialog({mode:"view",item})} className="text-blue-400"><Eye size={18}/></Button><Button permission="POSITION_UPDATE" title="Sửa" onClick={()=>setDialog({mode:"edit",item})} className="text-amber-400"><Pencil size={18}/></Button>{item.active ? <Button permission="POSITION_DELETE" title="Xóa mềm" onClick={()=>remove(item)} className="text-red-400"><Trash2 size={18}/></Button> : <Button permission="POSITION_UPDATE" title="Khôi phục" onClick={()=>restore(item)} className="text-emerald-400"><RotateCcw size={18}/></Button>}</div></td></tr>)}</tbody></table></div><Pagination currentPage={page} totalPages={Math.max(1,result.totalPages||1)} onPageChange={setPage}/></div>
    {dialog&&<PositionDialog {...dialog} departments={departments} onClose={()=>setDialog(null)} onSaved={refresh}/>}
  </div>;
}

function PositionDialog({ mode, item, departments, onClose, onSaved }) {
  const notification = useSystemNotification();
  const [form, setForm] = useState(item ? { ...EMPTY, ...item } : { ...EMPTY, departmentId: departments[0]?.id || "" });
  const [busy, setBusy] = useState(false);
  const [errors, setErrors] = useState({});
  const view = mode === "view";
  const set = (key) => (event) => {
    setForm((current) => ({ ...current, [key]: event.target.type === "checkbox" ? event.target.checked : event.target.value }));
    setErrors((current) => ({ ...current, [key]: undefined }));
  };
  const close = () => { if (!busy) onClose(); };

  const save = async () => {
    if (busy) return;
    const validation = validatePositionForm(form, { mode });
    setErrors(validation);
    if (Object.keys(validation).length) return;
    setBusy(true);
    try {
      if (mode === "create") await positionApi.create(toCreatePositionPayload(form));
      else if (!await updatePositionWithConfirmation({ original: item, form, api: positionApi, confirm: notification.confirm })) return;
      await onSaved(mode === "create" ? "Đã tạo vị trí." : "Đã cập nhật vị trí.");
    } catch (error) {
      notification.error(positionErrorMessage(error, { mode }));
    } finally {
      setBusy(false);
    }
  };

  const cls = "w-full rounded-2xl border border-zinc-700 bg-zinc-800 px-4 py-3 text-sm text-white outline-none focus:border-amber-400 disabled:opacity-70";
  return <AdminDialog open onClose={close} size="lg">
    <AdminDialogHeader title={view ? "Chi tiết vị trí" : mode === "create" ? "Thêm vị trí" : "Cập nhật vị trí"} onClose={close} />
    <AdminDialogBody>
      <div className="grid gap-5 sm:grid-cols-2">
        <div className="sm:col-span-2"><Field label="Phòng ban *" error={errors.departmentId}>
          <select value={form.departmentId} onChange={set("departmentId")} disabled={view || busy} aria-invalid={Boolean(errors.departmentId)} className={cls}>
            <option value="">-- Chọn phòng ban --</option>
            {item?.departmentId && !departments.some((department) => department.id === item.departmentId) && <option value={item.departmentId}>{item.departmentName}</option>}
            {departments.map((department) => <option key={department.id} value={department.id}>{department.code} · {department.name}</option>)}
          </select>
        </Field></div>
        <Field label={mode === "create" ? "Mã vị trí *" : "Mã vị trí"} error={errors.code}><input value={form.code} onChange={set("code")} disabled={mode !== "create" || busy} readOnly={mode !== "create"} aria-invalid={Boolean(errors.code)} className={cls} /></Field>
        <Field label="Tên vị trí *" error={errors.name}><input value={form.name} onChange={set("name")} disabled={view || busy} aria-invalid={Boolean(errors.name)} className={cls} /></Field>
        <Field label="Cấp bậc *" error={errors.hierarchyLevel}><input type="number" min="1" step="1" value={form.hierarchyLevel} onChange={set("hierarchyLevel")} disabled={view || busy} aria-invalid={Boolean(errors.hierarchyLevel)} className={cls} /></Field>
        <div className="hidden sm:block" />
        <Field label="Lương tối thiểu (VND) *" error={errors.minSalary}><input type="number" min="0" step="1" value={form.minSalary} onChange={set("minSalary")} disabled={view || busy} aria-invalid={Boolean(errors.minSalary)} className={cls} /></Field>
        <Field label="Lương tối đa (VND) *" error={errors.maxSalary}><input type="number" min="0" step="1" value={form.maxSalary} onChange={set("maxSalary")} disabled={view || busy} aria-invalid={Boolean(errors.maxSalary)} className={cls} /></Field>
        {view && <p className="text-sm text-zinc-300 sm:col-span-2">Khoảng lương: {formatSalaryRange(form.minSalary, form.maxSalary)}</p>}
        <div className="sm:col-span-2"><Field label="Mô tả"><textarea rows="4" value={form.description || ""} onChange={set("description")} disabled={view || busy} className={cls} /></Field></div>
        {!view && <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={form.active} onChange={set("active")} disabled={busy} className="h-4 w-4 accent-amber-500" />Đang hoạt động</label>}
      </div>
    </AdminDialogBody>
    <AdminDialogFooter className="justify-end gap-3">
      <Button onClick={close} disabled={busy} className="rounded-2xl px-6 py-3">{view ? "Đóng" : "Hủy"}</Button>
      {!view && <Button onClick={save} disabled={busy} className="rounded-2xl bg-amber-500 px-6 py-3 font-semibold text-zinc-950 disabled:opacity-50">{busy ? "Đang lưu..." : "Lưu vị trí"}</Button>}
    </AdminDialogFooter>
  </AdminDialog>;
}
function Badge({active}){return <span className={`rounded-full px-3 py-1 text-xs ${active?"bg-emerald-500/15 text-emerald-400":"bg-zinc-700 text-zinc-300"}`}>{active?"Hoạt động":"Ngừng hoạt động"}</span>}
function Field({label,error,children}){return <label className="grid gap-2 text-sm text-zinc-400"><span>{label}</span>{children}{error&&<span className="text-xs text-red-400">{error}</span>}</label>}
function Stat({label,value,icon:Icon,color}){return <div className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6"><div className="flex justify-between"><div><p className="text-zinc-400">{label}</p><p className={`mt-2 text-4xl font-bold ${color}`}>{value}</p></div><Icon size={38} className={color}/></div></div>}
