import { useCallback, useEffect, useState } from "react";
import { BriefcaseBusiness, Building, CircleCheck, Eye, Pencil, Plus, RotateCcw, Search, Trash2 } from "lucide-react";
import Button from "../../components/common/Button";
import Pagination from "../../components/common/Pagination";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../components/admin/common/AdminDialog";
import { useSystemNotification } from "../../components/common/SystemNotification";
import { departmentApi, positionApi } from "../../hooks/adminManagementApi";
import { buildDepartmentUpdateBody } from "./departmentManagementLogic";
import { formatSalaryRange, positionErrorMessage, toCreatePositionPayload, validatePositionForm } from "./positionManagementLogic";

const EMPTY_DEPARTMENT = { code: "", name: "", description: "", active: true };
const EMPTY_POSITION = { code: "", name: "", description: "", active: true, hierarchyLevel: "1", minSalary: "0", maxSalary: "0" };

export default function DepartmentManagement() {
  const notification = useSystemNotification();
  const [filters, setFilters] = useState({ keyword: "", active: "" });
  const [page, setPage] = useState(1);
  const [result, setResult] = useState({ content: [], totalElements: 0, totalPages: 1 });
  const [loading, setLoading] = useState(true);
  const [dialog, setDialog] = useState(null);

  const load = useCallback(async (signal) => {
    setLoading(true);
    try {
      setResult(await departmentApi.list(
        { ...filters, page: page - 1, size: 10, sort: "name,asc" },
        { signal },
      ));
    } catch (error) {
      if (error.name !== "AbortError") notification.error(error);
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, [filters, notification, page]);

  useEffect(() => {
    const controller = new AbortController();
    Promise.resolve().then(() => load(controller.signal));
    return () => controller.abort();
  }, [load]);

  const refresh = async (text) => {
    setDialog(null);
    notification.success(text);
    await load();
  };

  const remove = async (item) => {
    const accepted = await notification.confirm({
      title: "Xác nhận xóa phòng ban",
      message: `Bạn có chắc muốn xóa phòng ban ${item.name}? Phòng ban sẽ được chuyển sang trạng thái ngừng hoạt động.`,
      confirmText: "Xóa phòng ban",
      cancelText: "Hủy",
      destructive: true,
    });
    if (!accepted) return;
    try {
      await departmentApi.remove(item.id);
      await refresh("Đã xóa mềm phòng ban.");
    } catch (error) {
      notification.warning({ title: "Không thể xóa phòng ban", message: error.message });
    }
  };

  return (
    <div className="admin-catalog-page">
      <AdminCatalogPageHeader icon={Building} eyebrow="Cơ cấu tổ chức" title="Quản lý phòng ban" description="Quản lý cơ cấu phòng ban dùng chung cho hồ sơ nhân sự và tuyển dụng." />
      <div className="admin-catalog-toolbar mb-8 rounded-3xl border border-zinc-800 bg-zinc-900 p-6">
        <div className="flex flex-wrap gap-3">
          <div className="relative min-w-[17rem] flex-1">
            <input value={filters.keyword} onChange={(event) => { setPage(1); setFilters((current) => ({ ...current, keyword: event.target.value })); }} placeholder="Tìm theo tên hoặc mã phòng ban..." className="w-full rounded-2xl border border-zinc-700 bg-zinc-800 py-3 pl-11 pr-4 text-sm outline-none focus:border-amber-400" />
            <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" />
          </div>
          <StatusFilter value={filters.active} onChange={(value) => { setPage(1); setFilters((current) => ({ ...current, active: value })); }} />
          <Button onClick={() => { setPage(1); setFilters({ keyword: "", active: "" }); }} className="flex items-center gap-2 rounded-2xl bg-blue-500 px-5 py-3"><RotateCcw size={17} />Reset</Button>
          <Button permission="DEPARTMENT_CREATE" onClick={() => setDialog({ mode: "create", item: null })} className="flex items-center gap-2 rounded-2xl bg-amber-500 px-5 py-3 font-semibold text-zinc-950"><Plus size={17} />Thêm phòng ban</Button>
        </div>
      </div>
      <div className="mb-8 grid grid-cols-1 gap-5 sm:grid-cols-2">
        <Stat label="Tổng phòng ban" value={result.totalElements} icon={Building} color="text-blue-400" />
        <Stat label="Đang hoạt động trên trang" value={result.content.filter((item) => item.active).length} icon={CircleCheck} color="text-emerald-400" />
      </div>
      <CatalogTable title="Danh sách phòng ban" loading={loading} empty="Chưa có phòng ban phù hợp." columns={["Phòng ban", "Mô tả", "Trạng thái", "Thao tác"]}>
        {result.content.map((item) => (
          <tr key={item.id} className="hover:bg-zinc-800/60">
            <td className="px-6 py-5"><strong>{item.name}</strong><p className="mt-1 text-xs text-amber-400">{item.code}</p></td>
            <td className="max-w-md px-6 py-5 text-zinc-400">{item.description || "Chưa có mô tả"}</td>
            <td className="px-6 py-5 text-center"><Badge active={item.active} /></td>
            <td className="px-6 py-5"><div className="flex justify-center gap-3">
              <Button title="Xem" onClick={() => setDialog({ mode: "view", item })} className="text-blue-400"><Eye size={18} /></Button>
              <Button permission="DEPARTMENT_UPDATE" title="Sửa" onClick={() => setDialog({ mode: "edit", item })} className="text-amber-400"><Pencil size={18} /></Button>
              <Button permission="DEPARTMENT_DELETE" title="Xóa" onClick={() => remove(item)} className="text-red-400"><Trash2 size={18} /></Button>
            </div></td>
          </tr>
        ))}
      </CatalogTable>
      <Pagination currentPage={page} totalPages={Math.max(1, result.totalPages || 1)} onPageChange={setPage} />
      {dialog && <DepartmentDialog {...dialog} onClose={() => setDialog(null)} onSaved={refresh} />}
    </div>
  );
}

function DepartmentDialog({ mode, item, onClose, onSaved }) {
  const notification = useSystemNotification();
  const departmentId = item?.id;
  const [form, setForm] = useState(item ? { ...EMPTY_DEPARTMENT, ...item } : EMPTY_DEPARTMENT);
  const [positions, setPositions] = useState([]);
  const [positionsLoading, setPositionsLoading] = useState(mode !== "create");
  const [showPositionForm, setShowPositionForm] = useState(false);
  const [positionForm, setPositionForm] = useState(EMPTY_POSITION);
  const [positionErrors, setPositionErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const view = mode === "view";

  const loadDetails = useCallback(async (signal) => {
    if (!departmentId) return;
    setPositionsLoading(true);
    try {
      const detail = await departmentApi.detail(departmentId, { signal });
      setForm({ ...EMPTY_DEPARTMENT, ...detail });
      setPositions(detail.positions || []);
    } catch (error) {
      if (error.name !== "AbortError") {
        notification.error(error);
      }
    } finally {
      if (!signal?.aborted) setPositionsLoading(false);
    }
  }, [departmentId, notification]);

  useEffect(() => {
    if (!departmentId) return undefined;
    const controller = new AbortController();
    Promise.resolve().then(() => loadDetails(controller.signal));
    return () => controller.abort();
  }, [departmentId, loadDetails]);

  const set = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.type === "checkbox" ? event.target.checked : event.target.value }));
  const setPosition = (key) => (event) => {
    setPositionForm((current) => ({ ...current, [key]: event.target.type === "checkbox" ? event.target.checked : event.target.value }));
    setPositionErrors((current) => ({ ...current, [key]: undefined }));
  };

  const save = async () => {
    setBusy(true);
    try {
      if (mode === "create") {
        await departmentApi.create({ code: form.code.trim(), name: form.name.trim(), description: form.description.trim() || null, active: form.active });
      } else {
        await departmentApi.update(item.id, buildDepartmentUpdateBody(form, item.code));
      }
      await onSaved(mode === "create" ? "Đã tạo phòng ban." : "Đã cập nhật phòng ban.");
    } catch (error) {
      notification.error(error);
    } finally {
      setBusy(false);
    }
  };

  const addPosition = async () => {
    if (busy) return;
    const proposed = { ...positionForm, departmentId: item.id };
    const validation = validatePositionForm(proposed);
    setPositionErrors(validation);
    if (Object.keys(validation).length) return;
    setBusy(true);
    try {
      await positionApi.create(toCreatePositionPayload(proposed));
      notification.success("Đã thêm vị trí vào phòng ban.");
      setPositionForm(EMPTY_POSITION);
      setPositionErrors({});
      setShowPositionForm(false);
      await loadDetails();
    } catch (error) {
      notification.error(positionErrorMessage(error, { mode: "create" }));
    } finally {
      setBusy(false);
    }
  };

  const cls = "w-full rounded-2xl border border-zinc-700 bg-zinc-800 px-4 py-3 text-sm text-white outline-none focus:border-amber-400 disabled:cursor-not-allowed disabled:opacity-60";
  return (
    <AdminDialog open onClose={onClose} size="lg">
      <AdminDialogHeader title={view ? "Chi tiết phòng ban" : mode === "create" ? "Thêm phòng ban" : "Cập nhật phòng ban"} onClose={onClose} />
      <AdminDialogBody>
        <div className="grid gap-5 sm:grid-cols-2">
          <Field label="Mã phòng ban *"><input value={form.code} onChange={set("code")} disabled={view || mode === "edit"} className={cls} /></Field>
          <Field label="Tên phòng ban *"><input value={form.name} onChange={set("name")} disabled={view} className={cls} /></Field>
          <div className="sm:col-span-2"><Field label="Mô tả"><textarea rows="4" value={form.description || ""} onChange={set("description")} disabled={view} className={cls} /></Field></div>
          {mode === "create"
            ? <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={form.active} onChange={set("active")} className="h-4 w-4 accent-amber-500" />Đang hoạt động</label>
            : <div className="grid gap-2 text-sm text-zinc-400"><span>Trạng thái</span><div><Badge active={form.active} /></div></div>}
        </div>
        {mode !== "create" && <PositionSection view={view} positions={positions} loading={positionsLoading} errors={positionErrors} showForm={showPositionForm} setShowForm={setShowPositionForm} form={positionForm} setField={setPosition} busy={busy} onAdd={addPosition} inputClass={cls} />}
      </AdminDialogBody>
      <AdminDialogFooter className="justify-end gap-3">
        <Button onClick={onClose} className="rounded-2xl px-6 py-3">{view ? "Đóng" : "Hủy"}</Button>
        {!view && <Button onClick={save} disabled={busy || !form.code.trim() || !form.name.trim()} className="rounded-2xl bg-amber-500 px-6 py-3 font-semibold text-zinc-950 disabled:opacity-50">{busy ? "Đang lưu..." : "Lưu phòng ban"}</Button>}
      </AdminDialogFooter>
    </AdminDialog>
  );
}

function PositionSection({ view, positions, loading, errors, showForm, setShowForm, form, setField, busy, onAdd, inputClass }) {
  return (
    <section className="mt-8 overflow-hidden rounded-2xl border border-zinc-700">
      <header className="flex flex-wrap items-center justify-between gap-3 border-b border-zinc-700 bg-zinc-950 px-5 py-4">
        <div><h3 className="flex items-center gap-2 font-semibold"><BriefcaseBusiness size={18} />Vị trí thuộc phòng ban</h3><p className="mt-1 text-xs text-zinc-500">{positions.length} vị trí</p></div>
        {!view && <Button permission="POSITION_CREATE" disabled={busy} onClick={() => setShowForm((current) => !current)} className="flex items-center gap-2 rounded-xl bg-amber-500 px-4 py-2 text-sm font-semibold text-zinc-950"><Plus size={16} />Thêm vị trí</Button>}
      </header>
      {showForm && !view && <div className="grid gap-4 border-b border-zinc-700 bg-zinc-900 p-5 sm:grid-cols-2">
        <Field label="Mã vị trí *" error={errors.code}><input value={form.code} onChange={setField("code")} disabled={busy} aria-invalid={Boolean(errors.code)} className={inputClass} /></Field>
        <Field label="Tên vị trí *" error={errors.name}><input value={form.name} onChange={setField("name")} disabled={busy} aria-invalid={Boolean(errors.name)} className={inputClass} /></Field>
        <Field label="Cấp bậc *" error={errors.hierarchyLevel}><input type="number" min="1" step="1" value={form.hierarchyLevel} onChange={setField("hierarchyLevel")} disabled={busy} aria-invalid={Boolean(errors.hierarchyLevel)} className={inputClass} /></Field>
        <div className="hidden sm:block" />
        <Field label="Lương tối thiểu (VND) *" error={errors.minSalary}><input type="number" min="0" step="1" value={form.minSalary} onChange={setField("minSalary")} disabled={busy} aria-invalid={Boolean(errors.minSalary)} className={inputClass} /></Field>
        <Field label="Lương tối đa (VND) *" error={errors.maxSalary}><input type="number" min="0" step="1" value={form.maxSalary} onChange={setField("maxSalary")} disabled={busy} aria-invalid={Boolean(errors.maxSalary)} className={inputClass} /></Field>
        <div className="sm:col-span-2"><Field label="Mô tả"><textarea rows="2" value={form.description} onChange={setField("description")} disabled={busy} className={inputClass} /></Field></div>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={form.active} onChange={setField("active")} disabled={busy} className="h-4 w-4 accent-amber-500" />Đang hoạt động</label>
        <div className="flex justify-end gap-2 sm:col-start-2"><Button onClick={() => setShowForm(false)} disabled={busy} className="rounded-xl px-4 py-2">Hủy</Button><Button onClick={onAdd} disabled={busy} className="rounded-xl bg-amber-500 px-4 py-2 font-semibold text-zinc-950 disabled:opacity-50">{busy ? "Đang thêm..." : "Lưu vị trí"}</Button></div>
      </div>}
      <div className="overflow-x-auto"><table className="w-full text-sm">
        <thead className="bg-zinc-900 text-zinc-400"><tr><th className="px-5 py-3 text-left font-normal">Mã</th><th className="px-5 py-3 text-left font-normal">Tên vị trí</th><th className="px-5 py-3 text-left font-normal">Cấp bậc</th><th className="px-5 py-3 text-left font-normal">Khoảng lương (VND)</th><th className="px-5 py-3 text-left font-normal">Mô tả</th><th className="px-5 py-3 text-center font-normal">Trạng thái</th></tr></thead>
        <tbody className="divide-y divide-zinc-800">{loading
          ? <tr><td colSpan="6" className="px-5 py-8 text-center text-zinc-500">Đang tải vị trí...</td></tr>
          : positions.length === 0
            ? <tr><td colSpan="6" className="px-5 py-8 text-center text-zinc-500">Phòng ban chưa có vị trí.</td></tr>
            : positions.map((position) => <tr key={position.id}><td className="px-5 py-4 font-medium text-amber-400">{position.code}</td><td className="px-5 py-4">{position.name}</td><td className="px-5 py-4">{position.hierarchyLevel}</td><td className="whitespace-nowrap px-5 py-4">{formatSalaryRange(position.minSalary, position.maxSalary)}</td><td className="max-w-xs px-5 py-4 text-zinc-400">{position.description || "—"}</td><td className="px-5 py-4 text-center"><Badge active={position.active} /></td></tr>)}</tbody>
      </table></div>
    </section>
  );
}

function CatalogTable({ title, loading, empty, columns, children }) {
  return <div className="admin-catalog-table overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900"><div className="border-b border-zinc-800 bg-zinc-950 p-6"><h3 className="text-lg font-semibold">{title}</h3></div><div className="overflow-x-auto"><table className="w-full"><thead><tr className="border-b border-zinc-800 text-sm text-zinc-400">{columns.map((column) => <th key={column} className="px-6 py-4 text-left font-normal last:text-center">{column}</th>)}</tr></thead><tbody className="divide-y divide-zinc-800 text-sm">{loading ? <tr><td colSpan={columns.length} className="px-6 py-12 text-center text-zinc-500">Đang tải dữ liệu...</td></tr> : children.length === 0 ? <tr><td colSpan={columns.length} className="px-6 py-12 text-center text-zinc-500">{empty}</td></tr> : children}</tbody></table></div></div>;
}

function StatusFilter({ value, onChange }) {
  return <select value={value} onChange={(event) => onChange(event.target.value)} className="rounded-2xl border border-zinc-700 bg-zinc-800 px-4 py-3 text-sm"><option value="">Tất cả trạng thái</option><option value="true">Hoạt động</option><option value="false">Ngừng hoạt động</option></select>;
}

function Badge({ active }) {
  return <span className={`rounded-full px-3 py-1 text-xs ${active ? "bg-emerald-500/15 text-emerald-400" : "bg-zinc-700 text-zinc-300"}`}>{active ? "Hoạt động" : "Ngừng hoạt động"}</span>;
}

function Field({ label, error, children }) {
  return <label className="grid gap-2 text-sm text-zinc-400"><span>{label}</span>{children}{error && <span className="text-xs text-red-400">{error}</span>}</label>;
}

function Stat({ label, value, icon: Icon, color }) {
  return <div className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6"><div className="flex justify-between"><div><p className="text-zinc-400">{label}</p><p className={`mt-2 text-4xl font-bold ${color}`}>{value}</p></div><Icon size={38} className={color} /></div></div>;
}
