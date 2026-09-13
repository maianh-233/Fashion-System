import { useCallback, useEffect, useState } from "react";
import { Building2, CircleCheck, Eye, MapPin, Pencil, Phone, Plus, RotateCcw, Search, Trash2 } from "lucide-react";
import Button from "../../components/common/Button";
import Pagination from "../../components/common/Pagination";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../components/admin/common/AdminDialog";
import StoreLocationMap from "../../components/admin/Store/StoreLocationMap";
import { storeApi } from "../../hooks/adminManagementApi";

const EMPTY = { code: "", name: "", address: "", phone: "", latitude: "", longitude: "", active: true };
const PHONE_PATTERN = /^0(?:2\d{9}|[35789]\d{8})$/;
const SIX_DECIMAL_PATTERN = /^-?\d+(?:\.\d{1,6})?$/;

function normalizePhone(value) {
  const compact = value.trim().replace(/[\s().-]/g, "");
  return compact.startsWith("+84") ? `0${compact.slice(3)}` : compact;
}

function validateStore(form) {
  const errors = {};
  const latitude = Number(form.latitude);
  const longitude = Number(form.longitude);

  if (!form.name.trim()) errors.name = "Vui lòng nhập tên cửa hàng.";
  else if (form.name.trim().length > 255) errors.name = "Tên cửa hàng không được quá 255 ký tự.";

  if (!form.phone.trim()) errors.phone = "Vui lòng nhập số điện thoại liên hệ.";
  else if (!PHONE_PATTERN.test(normalizePhone(form.phone))) {
    errors.phone = "Dùng số Việt Nam hợp lệ, ví dụ 0901234567 hoặc 02838228899.";
  }

  if (form.latitude === "") errors.latitude = "Vui lòng nhập hoặc chọn vĩ độ trên bản đồ.";
  else if (!SIX_DECIMAL_PATTERN.test(String(form.latitude)) || !Number.isFinite(latitude) || latitude < -90 || latitude > 90) {
    errors.latitude = "Vĩ độ phải từ -90 đến 90 và tối đa 6 chữ số thập phân.";
  }
  if (form.longitude === "") errors.longitude = "Vui lòng nhập hoặc chọn kinh độ trên bản đồ.";
  else if (!SIX_DECIMAL_PATTERN.test(String(form.longitude)) || !Number.isFinite(longitude) || longitude < -180 || longitude > 180) {
    errors.longitude = "Kinh độ phải từ -180 đến 180 và tối đa 6 chữ số thập phân.";
  }

  return errors;
}

export default function StoreManagement() {
  const [filters, setFilters] = useState({ keyword: "", active: "" });
  const [page, setPage] = useState(1);
  const [result, setResult] = useState({ content: [], totalElements: 0, totalPages: 1 });
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState({ type: "", text: "" });
  const [dialog, setDialog] = useState(null);

  const load = useCallback(async (signal) => {
    setLoading(true);
    try { setResult(await storeApi.list({ ...filters, page: page - 1, size: 10, sort: "name,asc" }, { signal })); }
    catch (error) { if (error.name !== "AbortError") setNotice({ type: "error", text: error.message }); }
    finally { if (!signal?.aborted) setLoading(false); }
  }, [filters, page]);
  useEffect(() => { const controller = new AbortController(); Promise.resolve().then(() => load(controller.signal)); return () => controller.abort(); }, [load]);
  const refresh = async (text) => { setDialog(null); setNotice({ type: "success", text }); await load(); };
  const remove = async (store) => {
    if (!window.confirm(`Xóa cửa hàng ${store.name}? Cửa hàng có dữ liệu tham chiếu sẽ không thể xóa.`)) return;
    try { await storeApi.remove(store.id); await refresh("Đã xóa cửa hàng."); }
    catch (error) { setNotice({ type: "error", text: error.message }); }
  };

  return <div className="admin-catalog-page">
    <AdminCatalogPageHeader icon={Building2} eyebrow="Chỉ dành cho quản trị viên" title="Quản lý cửa hàng" description="Quản lý địa điểm vận hành và phạm vi phân công nhân viên trên toàn hệ thống." />
    {notice.text && <div className={`mb-5 rounded-2xl border px-4 py-3 text-sm ${notice.type === "error" ? "border-red-500/30 bg-red-500/10 text-red-300" : "border-emerald-500/30 bg-emerald-500/10 text-emerald-300"}`}>{notice.text}</div>}
    <div className="admin-catalog-toolbar mb-8 rounded-3xl border border-zinc-800 bg-zinc-900 p-6"><div className="flex flex-wrap gap-3">
      <div className="relative min-w-[17rem] flex-1"><input value={filters.keyword} onChange={(event)=>{setPage(1);setFilters((current)=>({...current,keyword:event.target.value}));}} placeholder="Tìm theo tên, mã hoặc số điện thoại..." className="w-full rounded-2xl border border-zinc-700 bg-zinc-800 py-3 pl-11 pr-4 text-sm outline-none focus:border-amber-400"/><Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500"/></div>
      <select value={filters.active} onChange={(event)=>{setPage(1);setFilters((current)=>({...current,active:event.target.value}));}} className="rounded-2xl border border-zinc-700 bg-zinc-800 px-4 py-3 text-sm"><option value="">Tất cả trạng thái</option><option value="true">Hoạt động</option><option value="false">Ngừng hoạt động</option></select>
      <Button onClick={()=>{setPage(1);setFilters({keyword:"",active:""});}} className="flex items-center gap-2 rounded-2xl bg-blue-500 px-5 py-3"><RotateCcw size={17}/>Reset</Button>
      <Button permission="STORE_CREATE" onClick={()=>setDialog({mode:"create",store:null})} className="flex items-center gap-2 rounded-2xl bg-amber-500 px-5 py-3 font-semibold text-zinc-950"><Plus size={17}/>Thêm cửa hàng</Button>
    </div></div>
    <div className="mb-8 grid grid-cols-1 gap-5 sm:grid-cols-2"><Stat label="Tổng cửa hàng" value={result.totalElements} icon={Building2} color="text-blue-400"/><Stat label="Đang hiển thị hoạt động" value={result.content.filter((store)=>store.active).length} icon={CircleCheck} color="text-emerald-400"/></div>
    <div className="admin-catalog-table overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900"><div className="flex justify-between border-b border-zinc-800 bg-zinc-950 p-6"><h3 className="text-lg font-semibold">Danh sách cửa hàng</h3><span className="text-sm text-zinc-400">{result.totalElements} kết quả</span></div>
      <div className="overflow-x-auto"><table className="w-full"><thead><tr className="border-b border-zinc-800 text-sm text-zinc-400"><th className="px-6 py-4 text-left font-normal">Cửa hàng</th><th className="px-6 py-4 text-left font-normal">Địa chỉ</th><th className="px-6 py-4 text-left font-normal">Liên hệ</th><th className="px-6 py-4 text-center font-normal">Trạng thái</th><th className="px-6 py-4 text-center font-normal">Thao tác</th></tr></thead><tbody className="divide-y divide-zinc-800 text-sm">
        {loading ? <tr><td colSpan="5" className="p-12 text-center text-zinc-500">Đang tải cửa hàng...</td></tr> : result.content.length === 0 ? <tr><td colSpan="5" className="p-12 text-center text-zinc-500">Chưa có cửa hàng phù hợp.</td></tr> : result.content.map((store)=><tr key={store.id} className="hover:bg-zinc-800/60"><td className="px-6 py-5"><strong>{store.name}</strong><p className="mt-1 text-xs font-medium text-amber-400">{store.code || "CHƯA CÓ MÃ"}</p></td><td className="px-6 py-5"><span className="inline-flex max-w-md items-start gap-2 text-zinc-300"><MapPin size={15} className="mt-0.5 shrink-0 text-amber-400"/>{store.address || "Chưa cập nhật"}</span></td><td className="px-6 py-5"><span className="inline-flex items-center gap-2 text-zinc-300"><Phone size={15} className="text-amber-400"/>{store.phone || "Chưa cập nhật"}</span></td><td className="px-6 py-5 text-center"><span className={`rounded-full px-3 py-1 text-xs ${store.active?"bg-emerald-500/15 text-emerald-400":"bg-zinc-700 text-zinc-300"}`}>{store.active?"Hoạt động":"Ngừng hoạt động"}</span></td><td className="px-6 py-5"><div className="flex justify-center gap-4"><Button title="Xem" onClick={()=>setDialog({mode:"view",store})} className="text-blue-400"><Eye size={18}/></Button><Button permission="STORE_UPDATE" title="Sửa" onClick={()=>setDialog({mode:"edit",store})} className="text-amber-400"><Pencil size={18}/></Button><Button permission="STORE_DELETE" title="Xóa" onClick={()=>remove(store)} className="text-red-400"><Trash2 size={18}/></Button></div></td></tr>)}
      </tbody></table></div><Pagination currentPage={page} totalPages={Math.max(1,result.totalPages||1)} onPageChange={setPage}/></div>
    {dialog && <StoreDialog {...dialog} onClose={()=>setDialog(null)} onSaved={refresh}/>} 
  </div>;
}

function StoreDialog({ mode, store, onClose, onSaved }) {
  const [form, setForm] = useState(store ? { ...EMPTY, ...store, latitude: store.latitude ?? "", longitude: store.longitude ?? "" } : EMPTY);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [errors, setErrors] = useState({});
  const view = mode === "view";
  const set = (key) => (event) => {
    const value = event.target.type === "checkbox" ? event.target.checked : event.target.value;
    setForm((current) => ({ ...current, [key]: value }));
    setErrors((current) => ({ ...current, [key]: "" }));
  };
  const setCoordinates = ({ latitude, longitude }) => {
    setForm((current) => ({ ...current, latitude, longitude }));
    setErrors((current) => ({ ...current, latitude: "", longitude: "" }));
  };
  const normalizePhoneField = () => {
    if (!form.phone.trim()) return;
    setForm((current) => ({ ...current, phone: normalizePhone(current.phone) }));
  };
  const save = async () => {
    const nextErrors = validateStore(form);
    setErrors(nextErrors);
    setError("");
    if (Object.keys(nextErrors).length) return;

    setBusy(true);
    try {
      const latitude = Number(form.latitude);
      const longitude = Number(form.longitude);
      const availability = await storeApi.coordinateAvailability({
        latitude,
        longitude,
        ...(mode === "edit" ? { excludeId: store.id } : {}),
      });
      if (!availability.available) {
        const duplicateMessage = "Tọa độ này đã thuộc về một cửa hàng khác.";
        setErrors((current) => ({ ...current, latitude: duplicateMessage, longitude: duplicateMessage }));
        setError(duplicateMessage);
        return;
      }

      const body = {
        name: form.name.trim(),
        address: form.address.trim() || null,
        phone: normalizePhone(form.phone),
        latitude,
        longitude,
        active: form.active,
      };
      const saved = mode === "create"
        ? await storeApi.create(body)
        : await storeApi.update(store.id, body);
      await onSaved(mode === "create"
        ? `Đã tạo cửa hàng với mã ${saved.code}.`
        : `Đã cập nhật cửa hàng ${saved.code}.`);
    } catch (exception) {
      setError(exception.message);
    } finally {
      setBusy(false);
    }
  };
  const cls="w-full rounded-2xl border border-zinc-700 bg-zinc-800 px-4 py-3 text-sm text-white outline-none focus:border-amber-400 disabled:opacity-70";
  const codeValue = mode === "create" ? "Tự động tạo khi lưu" : form.code;
  return <AdminDialog open onClose={onClose} size="lg"><AdminDialogHeader><div><p className="text-xs font-bold uppercase tracking-widest text-amber-400">Quản lý cửa hàng</p><h2 className="mt-2 text-2xl font-semibold">{view?"Chi tiết cửa hàng":mode==="create"?"Thêm cửa hàng":"Cập nhật cửa hàng"}</h2></div></AdminDialogHeader><AdminDialogBody>{error&&<div className="mb-5 rounded-2xl border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-300">{error}</div>}<div className="grid grid-cols-1 gap-5 sm:grid-cols-2"><Field label="Mã cửa hàng" hint="Mã do hệ thống tự sinh và không thể chỉnh sửa."><input value={codeValue} disabled aria-readonly="true" className={cls}/></Field><Field label="Tên cửa hàng *" error={errors.name}><input required maxLength="255" value={form.name} onChange={set("name")} disabled={view} className={cls}/></Field><div className="sm:col-span-2"><Field label="Địa chỉ"><textarea rows="3" value={form.address} onChange={set("address")} disabled={view} className={cls}/></Field></div><Field label="Số điện thoại liên hệ *" error={errors.phone} hint="Ví dụ: 0901234567 hoặc 02838228899"><input required inputMode="tel" maxLength="20" value={form.phone} onChange={set("phone")} onBlur={normalizePhoneField} disabled={view} className={cls}/></Field><Field label="Vĩ độ *" error={errors.latitude}><input required type="number" min="-90" max="90" step="0.000001" value={form.latitude} onChange={set("latitude")} disabled={view} className={cls}/></Field><Field label="Kinh độ *" error={errors.longitude}><input required type="number" min="-180" max="180" step="0.000001" value={form.longitude} onChange={set("longitude")} disabled={view} className={cls}/></Field>{!view&&<label className="flex items-center gap-2 self-end pb-3 text-sm"><input type="checkbox" checked={form.active} onChange={set("active")} className="h-4 w-4 accent-amber-500"/>Đang hoạt động</label>}<div className="sm:col-span-2"><StoreLocationMap latitude={form.latitude} longitude={form.longitude} readOnly={view} onCoordinatesChange={setCoordinates}/></div></div></AdminDialogBody><AdminDialogFooter className="justify-end gap-3"><Button onClick={onClose} disabled={busy} className="rounded-2xl px-6 py-3">{view?"Đóng":"Hủy"}</Button>{!view&&<Button onClick={save} disabled={busy} className="rounded-2xl bg-amber-500 px-6 py-3 font-semibold text-zinc-950 disabled:opacity-50">{busy?"Đang kiểm tra...":"Lưu cửa hàng"}</Button>}</AdminDialogFooter></AdminDialog>;
}
function Field({label,hint,error,children}){return <label className="grid gap-2 text-sm text-zinc-400"><span>{label}</span>{children}{error?<span className="text-xs text-red-400">{error}</span>:hint?<span className="text-xs text-zinc-500">{hint}</span>:null}</label>}
function Stat({label,value,icon:Icon,color}){return <div className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6"><div className="flex justify-between"><div><p className="text-zinc-400">{label}</p><p className={`mt-2 text-4xl font-bold ${color}`}>{value}</p></div><Icon size={38} className={color}/></div></div>}
