import { useState } from "react";
import { Download, Lock, Plus, Search } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import Pagination from "../../../components/common/Pagination";
import { attendanceApi, saveBlob } from "../../../api/admin/attendanceApi";
import {
  Badge, Empty, ErrorText, Field, Loading, Section, btnGhost, btnPrimary, card, control, textarea, useLoader,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { useAttendance } from "./AttendanceLayout";
import {
  RECORD_STATUS, SOURCE_LABEL, currentMonth, formatDate, formatDateTime, formatTime, fromLocalDateTime, isoDate, minutesText,
  monthRange, shortTime, toLocalDateTime,
} from "./attendanceLogic";

const STATUS_FILTERS = [
  ["", "Tất cả trạng thái"], ["COMPLETED", "Đủ công"], ["WORKING", "Đang làm"], ["MISSING_CHECK_OUT", "Thiếu check-out"],
  ["MISSING_CHECK_IN", "Thiếu check-in"], ["LATE", "Đi trễ"], ["EARLY", "Về sớm"],
];

export default function AttendanceRecords() {
  const { can, stores, scopeStoreId } = useAttendance();
  const [filter, setFilter] = useState({ month: currentMonth(), storeId: "", employeeType: "", status: "", keyword: "" });
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);
  const [selected, setSelected] = useState(null);
  const [creating, setCreating] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState("");
  const range = monthRange(filter.month);
  const params = { ...range, storeId: filter.storeId, employeeType: filter.employeeType, status: filter.status, keyword: filter.keyword };
  const { data, error, loading, reload } = useLoader(
    (signal) => attendanceApi.records({ ...params, page: page - 1, size: 20 }, { signal }), [JSON.stringify(params), page]);

  const update = (patch) => { setFilter((f) => ({ ...f, ...patch })); setPage(1); };
  const exportExcel = async () => {
    setExporting(true);
    setExportError("");
    try {
      saveBlob(await attendanceApi.exportRecords(params), `bang-cong_${filter.month}.xlsx`);
    } catch (e) {
      setExportError(e.message);
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="space-y-4">
      <Section>
        <form className="grid gap-3 md:grid-cols-6" onSubmit={(e) => { e.preventDefault(); update({ keyword: keyword.trim() }); }}>
          <Field label="Tháng"><input type="month" className={control} value={filter.month} onChange={(e) => update({ month: e.target.value || currentMonth() })} /></Field>
          <Field label="Cửa hàng">
            <select className={control} value={filter.storeId} onChange={(e) => update({ storeId: e.target.value })} disabled={Boolean(scopeStoreId)}>
              {!scopeStoreId && <option value="">Tất cả</option>}
              {stores.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </Field>
          <Field label="Loại nhân viên">
            <select className={control} value={filter.employeeType} onChange={(e) => update({ employeeType: e.target.value })}>
              <option value="">Tất cả</option><option value="STORE">Nhân viên cửa hàng</option><option value="GLOBAL">Nhân viên Global</option>
            </select>
          </Field>
          <Field label="Trạng thái">
            <select className={control} value={filter.status} onChange={(e) => update({ status: e.target.value })}>
              {STATUS_FILTERS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
            </select>
          </Field>
          <Field label="Nhân viên" className="md:col-span-2">
            <div className="flex gap-2">
              <input className={control} placeholder="Mã hoặc tên nhân viên" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
              <button type="submit" className={btnGhost} aria-label="Tìm"><Search size={15} /></button>
            </div>
          </Field>
        </form>
        <div className="mt-4 flex flex-wrap justify-end gap-2">
          {can.manage && <button type="button" className={btnGhost} onClick={() => setCreating(true)}><Plus size={15} />Bổ sung công</button>}
          {can.export && <button type="button" className={btnPrimary} onClick={exportExcel} disabled={exporting}><Download size={15} />{exporting ? "Đang xuất…" : "Xuất Excel"}</button>}
        </div>
        <div className="mt-2"><ErrorText>{exportError}</ErrorText></div>
      </Section>

      <ErrorText>{error}</ErrorText>
      <div className={`${card} overflow-x-auto`}>
        <table className="min-w-full text-sm">
          <thead className="border-b border-zinc-800 text-left text-xs uppercase tracking-wide text-zinc-400">
            <tr>
              <th className="px-4 py-3">Ngày</th><th className="px-4 py-3">Nhân viên</th><th className="px-4 py-3">Cửa hàng</th>
              <th className="px-4 py-3">Ca</th><th className="px-4 py-3">Vào</th><th className="px-4 py-3">Ra</th>
              <th className="px-4 py-3">Thời gian</th><th className="px-4 py-3">Trễ / sớm</th><th className="px-4 py-3">Trạng thái</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-800">
            {loading && !data && <tr><td colSpan="9"><Loading /></td></tr>}
            {data?.content?.length === 0 && <tr><td colSpan="9"><Empty>Không có bản ghi chấm công trong tháng này.</Empty></td></tr>}
            {data?.content?.map((r) => (
              <tr key={r.id} className="cursor-pointer hover:bg-zinc-800/40" onClick={() => setSelected(r.id)}>
                <td className="whitespace-nowrap px-4 py-3">{formatDate(r.date)}</td>
                <td className="px-4 py-3"><div className="font-medium">{r.employeeName}</div><div className="text-xs text-zinc-500">{r.employeeCode} · {r.employeeType === "STORE" ? "Cửa hàng" : "Global"}</div></td>
                <td className="px-4 py-3 text-zinc-300">{r.storeName || "—"}</td>
                <td className="whitespace-nowrap px-4 py-3 text-zinc-400">{shortTime(r.expectedStart)}–{shortTime(r.expectedEnd)}</td>
                <td className="px-4 py-3 tabular-nums">{formatTime(r.checkIn)}</td>
                <td className="px-4 py-3 tabular-nums">{formatTime(r.checkOut)}</td>
                <td className="px-4 py-3 tabular-nums">{minutesText(r.workedMinutes)}</td>
                <td className="whitespace-nowrap px-4 py-3 text-xs">
                  {r.lateMinutes > 0 && <span className="text-amber-300">Trễ {r.lateMinutes}p </span>}
                  {r.earlyMinutes > 0 && <span className="text-amber-300">Sớm {r.earlyMinutes}p</span>}
                  {!r.lateMinutes && !r.earlyMinutes && <span className="text-zinc-500">—</span>}
                </td>
                <td className="px-4 py-3"><div className="flex items-center gap-1.5"><Badge map={RECORD_STATUS} value={r.status} />{r.locked && <Lock size={13} className="text-zinc-500" aria-label="Đã chốt lương" />}</div></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination currentPage={page} totalPages={data?.totalPages || 1} onPageChange={setPage} />

      {selected && <RecordDialog id={selected} canManage={can.manage} onClose={() => setSelected(null)} onSaved={reload} />}
      {creating && <CreateRecordDialog onClose={() => setCreating(false)} onSaved={() => { setCreating(false); reload(); }} />}
    </div>
  );
}

function RecordDialog({ id, canManage, onClose, onSaved }) {
  const { data, error, loading, reload } = useLoader((signal) => attendanceApi.record(id, { signal }), [id]);
  const [form, setForm] = useState(null);
  const [saveError, setSaveError] = useState("");
  const [saving, setSaving] = useState(false);
  const record = data?.record;
  const editing = form !== null;

  const startEdit = () => setForm({ checkIn: fromLocalDateTime(record.checkIn), checkOut: fromLocalDateTime(record.checkOut), reason: "" });
  const save = async (event) => {
    event.preventDefault();
    if (!form.reason.trim()) return setSaveError("Vui lòng nhập lý do điều chỉnh");
    setSaving(true);
    setSaveError("");
    try {
      await attendanceApi.adjust(id, { checkIn: toLocalDateTime(form.checkIn), checkOut: toLocalDateTime(form.checkOut), reason: form.reason.trim() });
      setForm(null);
      reload();
      onSaved();
    } catch (e) {
      setSaveError(e.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <AdminDialog onClose={onClose} size="lg">
      <AdminDialogHeader title="Chi tiết chấm công" description={record ? `${record.employeeName} · ${formatDate(record.date)}` : ""} onClose={onClose} />
      <AdminDialogBody className="space-y-4">
        <ErrorText>{error}</ErrorText>
        {loading && !data && <Loading />}
        {record && (
          <>
            <dl className="grid grid-cols-2 gap-3 text-sm md:grid-cols-3">
              <Info label="Ca dự kiến" value={`${shortTime(record.expectedStart)} – ${shortTime(record.expectedEnd)}`} />
              <Info label="Giờ vào" value={formatDateTime(record.checkIn) || "—"} />
              <Info label="Giờ ra" value={formatDateTime(record.checkOut) || "—"} />
              <Info label="Vị trí vào" value={point(record.checkInLatitude, record.checkInLongitude, record.checkInDistanceM)} />
              <Info label="Vị trí ra" value={point(record.checkOutLatitude, record.checkOutLongitude, record.checkOutDistanceM)} />
              <Info label="Nguồn" value={SOURCE_LABEL[record.source] || record.source} />
            </dl>
            {record.locked && <p className="rounded-xl border border-zinc-700 bg-zinc-800/50 px-3 py-2 text-sm text-zinc-300"><Lock size={14} className="mr-1 inline" />Tháng này đã chốt lương, không điều chỉnh được.</p>}
            {editing ? (
              <form className="space-y-3 rounded-2xl border border-zinc-800 p-4" onSubmit={save}>
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field label="Giờ vào"><input type="datetime-local" className={control} value={form.checkIn} onChange={(e) => setForm({ ...form, checkIn: e.target.value })} /></Field>
                  <Field label="Giờ ra"><input type="datetime-local" className={control} value={form.checkOut} onChange={(e) => setForm({ ...form, checkOut: e.target.value })} /></Field>
                </div>
                <Field label="Lý do điều chỉnh" required><textarea className={textarea} rows={3} maxLength={1000} value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} /></Field>
                <ErrorText>{saveError}</ErrorText>
                <div className="flex justify-end gap-2">
                  <button type="button" className={btnGhost} onClick={() => setForm(null)}>Hủy</button>
                  <button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : "Lưu điều chỉnh"}</button>
                </div>
              </form>
            ) : canManage && !record.locked && (
              <button type="button" className={btnGhost} onClick={startEdit}>Điều chỉnh giờ công</button>
            )}
            <div>
              <h3 className="mb-2 text-sm font-semibold text-zinc-200">Lịch sử thay đổi</h3>
              {data.history.length === 0 ? <Empty>Chưa có lịch sử.</Empty> : (
                <ul className="space-y-2">
                  {data.history.map((h, i) => (
                    <li key={i} className="rounded-xl border border-zinc-800 px-3 py-2 text-sm">
                      <div className="flex flex-wrap justify-between gap-2"><span className="font-medium">{h.action}</span><span className="text-xs text-zinc-500">{formatDateTime(h.createdAt)} · {h.actorName || "Hệ thống"}</span></div>
                      {h.reason && <p className="mt-1 text-zinc-300">{h.reason}</p>}
                      {(h.oldData || h.newData) && <p className="mt-1 text-xs text-zinc-500">{summary(h.oldData)} → {summary(h.newData)}</p>}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </>
        )}
      </AdminDialogBody>
      <AdminDialogFooter><button type="button" className={btnGhost} onClick={onClose}>Đóng</button></AdminDialogFooter>
    </AdminDialog>
  );
}

function CreateRecordDialog({ onClose, onSaved }) {
  const [keyword, setKeyword] = useState("");
  const [form, setForm] = useState({ userId: "", date: isoDate(), checkIn: "", checkOut: "", reason: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const employees = useLoader((signal) => attendanceApi.employees({ keyword }, { signal }), [keyword]);
  const submit = async (event) => {
    event.preventDefault();
    if (!form.userId || !form.checkIn || !form.checkOut || !form.reason.trim()) return setError("Vui lòng nhập đủ nhân viên, giờ vào, giờ ra và lý do");
    setSaving(true);
    setError("");
    try {
      await attendanceApi.createRecord({ userId: form.userId, date: form.date, checkIn: `${form.date}T${form.checkIn}:00`, checkOut: `${form.date}T${form.checkOut}:00`, reason: form.reason.trim() });
      onSaved();
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };
  return (
    <AdminDialog onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title="Bổ sung ngày công" description="Dùng khi nhân viên có đi làm nhưng không có bản ghi (ví dụ thiết bị lỗi). Thao tác được lưu lịch sử." onClose={onClose} />
        <AdminDialogBody className="space-y-3">
          <Field label="Tìm nhân viên"><input className={control} value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Mã hoặc tên" /></Field>
          <Field label="Nhân viên" required>
            <select className={control} value={form.userId} onChange={(e) => setForm({ ...form, userId: e.target.value })}>
              <option value="">— Chọn —</option>
              {(employees.data || []).map((u) => <option key={u.id} value={u.id}>{u.employeeCode || "—"} · {u.fullName}{u.storeName ? ` (${u.storeName})` : ""}</option>)}
            </select>
          </Field>
          <div className="grid gap-3 sm:grid-cols-3">
            <Field label="Ngày" required><input type="date" className={control} max={isoDate()} value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} /></Field>
            <Field label="Giờ vào" required><input type="time" className={control} value={form.checkIn} onChange={(e) => setForm({ ...form, checkIn: e.target.value })} /></Field>
            <Field label="Giờ ra" required><input type="time" className={control} value={form.checkOut} onChange={(e) => setForm({ ...form, checkOut: e.target.value })} /></Field>
          </div>
          <Field label="Lý do" required><textarea className={textarea} rows={3} maxLength={1000} value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} /></Field>
          <ErrorText>{error}</ErrorText>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : "Lưu"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}

function Info({ label, value }) {
  return <div><dt className="text-xs text-zinc-500">{label}</dt><dd className="mt-0.5 text-zinc-200">{value}</dd></div>;
}

function point(lat, lon, distance) {
  if (lat == null || lon == null) return "Không có";
  return `${lat}, ${lon}${distance != null ? ` (cách cửa hàng ${distance} m)` : ""}`;
}

function summary(text) {
  if (!text) return "—";
  try {
    const d = JSON.parse(text);
    return `${formatTime(d.checkIn)}–${formatTime(d.checkOut)}`;
  } catch {
    return "—";
  }
}
