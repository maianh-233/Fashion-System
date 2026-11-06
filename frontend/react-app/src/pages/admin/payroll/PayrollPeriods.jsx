import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Plus } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import Pagination from "../../../components/common/Pagination";
import { payrollApi } from "../../../api/admin/payrollApi";
import { Badge, Empty, ErrorText, Field, Loading, Section, btnGhost, btnPrimary, card, control, textarea, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import { currentMonth } from "../attendance/attendanceLogic";
import { usePayroll } from "./PayrollLayout";
import { PERIOD_STATUS, money, monthLabel } from "./payrollLogic";

export default function PayrollPeriods() {
  const { can, stores, scopeStoreId } = usePayroll();
  const [filter, setFilter] = useState({ month: "", storeId: "", status: "" });
  const [page, setPage] = useState(1);
  const [creating, setCreating] = useState(false);
  const { data, error, loading } = useLoader((signal) => payrollApi.periods({ ...filter, page: page - 1, size: 20 }, { signal }), [JSON.stringify(filter), page]);
  const update = (patch) => { setFilter((f) => ({ ...f, ...patch })); setPage(1); };

  return (
    <div className="space-y-4">
      <Section>
        <div className="grid gap-3 md:grid-cols-4">
          <Field label="Tháng"><input type="month" className={control} value={filter.month} onChange={(e) => update({ month: e.target.value })} /></Field>
          <Field label="Cửa hàng">
            <select className={control} value={filter.storeId} onChange={(e) => update({ storeId: e.target.value })} disabled={Boolean(scopeStoreId)}>
              {!scopeStoreId && <option value="">Tất cả</option>}
              {stores.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </Field>
          <Field label="Trạng thái">
            <select className={control} value={filter.status} onChange={(e) => update({ status: e.target.value })}>
              <option value="">Tất cả</option>
              {Object.entries(PERIOD_STATUS).map(([v, s]) => <option key={v} value={v}>{s.label}</option>)}
            </select>
          </Field>
          <div className="flex items-end justify-end">
            {can.manage && <button type="button" className={btnPrimary} onClick={() => setCreating(true)}><Plus size={15} />Tạo kỳ lương</button>}
          </div>
        </div>
      </Section>
      <ErrorText>{error}</ErrorText>
      <div className={`${card} overflow-x-auto`}>
        <table className="min-w-full text-sm">
          <thead className="border-b border-zinc-800 text-left text-xs uppercase tracking-wide text-zinc-400">
            <tr><th className="px-4 py-3">Kỳ lương</th><th className="px-4 py-3">Phạm vi</th><th className="px-4 py-3 text-right">Nhân viên</th>
              <th className="px-4 py-3 text-right">Tổng thu nhập</th><th className="px-4 py-3 text-right">Thực nhận</th><th className="px-4 py-3">Trạng thái</th><th className="px-4 py-3">Người tạo</th></tr>
          </thead>
          <tbody className="divide-y divide-zinc-800">
            {loading && !data && <tr><td colSpan="7"><Loading /></td></tr>}
            {data?.content?.length === 0 && <tr><td colSpan="7"><Empty>Chưa có kỳ lương nào.</Empty></td></tr>}
            {data?.content?.map((p) => (
              <tr key={p.id} className="hover:bg-zinc-800/40">
                <td className="px-4 py-3"><Link to={`/admin/payroll/periods/${p.id}`} className="font-medium text-amber-200 hover:underline">Tháng {monthLabel(p.periodMonth)}</Link><div className="text-xs text-zinc-500">{p.code}</div></td>
                <td className="px-4 py-3">{p.storeName || "Văn phòng (Global)"}</td>
                <td className="px-4 py-3 text-right tabular-nums">{p.employeeCount}{p.needsReviewCount > 0 && <span className="ml-1 text-xs text-amber-300">({p.needsReviewCount} cần xem)</span>}</td>
                <td className="px-4 py-3 text-right tabular-nums">{money(p.totalGross)}</td>
                <td className="px-4 py-3 text-right tabular-nums">{money(p.totalNet)}</td>
                <td className="px-4 py-3"><Badge map={PERIOD_STATUS} value={p.status} /></td>
                <td className="px-4 py-3 text-xs text-zinc-400">{p.createdByName}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination currentPage={page} totalPages={data?.totalPages || 1} onPageChange={setPage} />
      {creating && <CreatePeriodDialog onClose={() => setCreating(false)} />}
    </div>
  );
}

function CreatePeriodDialog({ onClose }) {
  const { stores, scopeStoreId } = usePayroll();
  const navigate = useNavigate();
  const [form, setForm] = useState({ month: currentMonth(), storeId: scopeStoreId || "", note: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const created = await payrollApi.createPeriod({ month: form.month, storeId: form.storeId || null, note: form.note.trim() || null });
      navigate(`/admin/payroll/periods/${created.period.id}`);
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };
  return (
    <AdminDialog onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title="Tạo kỳ lương" description="Mỗi tháng có một kỳ lương cho từng cửa hàng và một kỳ cho Văn phòng (nhân viên Global)." onClose={onClose} />
        <AdminDialogBody className="space-y-3">
          <Field label="Tháng lương" required><input type="month" className={control} value={form.month} onChange={(e) => setForm({ ...form, month: e.target.value })} required /></Field>
          <Field label="Phạm vi" required>
            <select className={control} value={form.storeId} onChange={(e) => setForm({ ...form, storeId: e.target.value })} disabled={Boolean(scopeStoreId)}>
              {!scopeStoreId && <option value="">Văn phòng (nhân viên Global)</option>}
              {stores.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </Field>
          <Field label="Ghi chú"><textarea className={textarea} rows={2} value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} /></Field>
          <ErrorText>{error}</ErrorText>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang tạo…" : "Tạo kỳ lương"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}
