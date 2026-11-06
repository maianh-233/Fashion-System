import { useState } from "react";
import { Link } from "react-router-dom";
import { Download } from "lucide-react";
import { payrollApi } from "../../../api/admin/payrollApi";
import { saveBlob } from "../../../api/admin/attendanceApi";
import { Empty, ErrorText, Field, Loading, Section, btnPrimary, control, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import { currentMonth } from "../attendance/attendanceLogic";
import { usePayroll } from "./PayrollLayout";
import { money } from "./payrollLogic";

export default function PayrollReports() {
  const { can, stores, scopeStoreId } = usePayroll();
  const [month, setMonth] = useState(currentMonth());
  const [storeId, setStoreId] = useState("");
  const [exportError, setExportError] = useState("");
  const params = { month, storeId };
  const key = [month, storeId];
  const byMonth = useLoader((signal) => payrollApi.report("by-month", params, { signal }), key);
  const byStore = useLoader((signal) => payrollApi.report("by-store", params, { signal }), key);
  const byDept = useLoader((signal) => payrollApi.report("by-department", params, { signal }), key);
  const missing = useLoader((signal) => payrollApi.report("missing-salary", { storeId }, { signal }), [storeId]);
  const incomplete = useLoader((signal) => payrollApi.report("incomplete", params, { signal }), key);

  return (
    <div className="space-y-5">
      <Section>
        <div className="grid gap-3 sm:grid-cols-4">
          <Field label="Tháng"><input type="month" className={control} value={month} onChange={(e) => setMonth(e.target.value || currentMonth())} /></Field>
          <Field label="Cửa hàng">
            <select className={control} value={storeId} onChange={(e) => setStoreId(e.target.value)} disabled={Boolean(scopeStoreId)}>
              {!scopeStoreId && <option value="">Toàn chuỗi</option>}
              {stores.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </Field>
          <div className="flex items-end justify-end sm:col-span-2">
            {can.export && <button type="button" className={btnPrimary} onClick={async () => {
              setExportError("");
              try { saveBlob(await payrollApi.exportReports(params), `bao-cao-luong_${month}.xlsx`); } catch (e) { setExportError(e.message); }
            }}><Download size={15} />Xuất Excel</button>}
          </div>
        </div>
        <div className="mt-2"><ErrorText>{exportError}</ErrorText></div>
      </Section>

      <Section title="Quỹ lương 12 tháng gần nhất"><MonthChart loader={byMonth} /></Section>
      <div className="grid gap-5 xl:grid-cols-2 [&>*]:min-w-0">
        <Section title="Chi phí lương theo cửa hàng"><CostTable loader={byStore} /></Section>
        <Section title="Chi phí lương theo phòng ban"><CostTable loader={byDept} /></Section>
      </div>
      <div className="grid gap-5 xl:grid-cols-2 [&>*]:min-w-0">
        <Section title="Nhân viên chưa có cấu hình lương"><Issues loader={missing} link={(i) => `/admin/payroll/salaries/${i.userId}`} /></Section>
        <Section title="Nhân viên chưa đủ dữ liệu tính lương"><Issues loader={incomplete} /></Section>
      </div>
    </div>
  );
}

function MonthChart({ loader }) {
  if (loader.loading && !loader.data) return <Loading />;
  if (loader.error) return <ErrorText>{loader.error}</ErrorText>;
  const rows = loader.data || [];
  if (rows.length === 0) return <Empty>Chưa có dữ liệu lương.</Empty>;
  const max = Math.max(...rows.map((r) => Number(r.totalGross)), 1);
  return (
    <div className="space-y-2">
      {rows.map((r, i) => {
        const prev = rows[i - 1];
        const change = prev && Number(prev.totalGross) > 0 ? ((Number(r.totalGross) - Number(prev.totalGross)) / Number(prev.totalGross)) * 100 : null;
        return (
          <div key={r.label} className="grid grid-cols-[70px_1fr_auto] items-center gap-3 text-sm">
            <span className="tabular-nums text-zinc-400">{r.label}</span>
            <div className="h-6 rounded-lg bg-zinc-800">
              <div className="h-6 rounded-lg bg-amber-400/80" style={{ width: `${(Number(r.totalGross) / max) * 100}%` }} title={`Thực nhận ${money(r.totalNet)}`} />
            </div>
            <span className="w-56 text-right tabular-nums">
              {money(r.totalGross)} <span className="text-xs text-zinc-500">· {r.employeeCount} NV</span>
              {change != null && <span className={`ml-1 text-xs ${change >= 0 ? "text-emerald-300" : "text-rose-300"}`}>{change >= 0 ? "+" : ""}{change.toFixed(1)}%</span>}
            </span>
          </div>
        );
      })}
      <p className="text-xs text-zinc-500">Tổng quỹ lương = lương theo công + phụ cấp + thưởng + tăng ca (trước khấu trừ). Không tính kỳ đã hủy.</p>
    </div>
  );
}

function CostTable({ loader }) {
  if (loader.loading && !loader.data) return <Loading />;
  if (loader.error) return <ErrorText>{loader.error}</ErrorText>;
  const rows = loader.data || [];
  if (rows.length === 0) return <Empty>Chưa có bảng lương trong tháng.</Empty>;
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full whitespace-nowrap text-sm">
        <thead className="text-left text-xs uppercase text-zinc-400"><tr><th className="py-2 pr-3">Nhóm</th><th className="py-2 pr-3 text-right">NV</th><th className="py-2 pr-3 text-right">Thu nhập</th><th className="py-2 pr-3 text-right">Khấu trừ</th><th className="py-2 text-right">Thực nhận</th></tr></thead>
        <tbody className="divide-y divide-zinc-800">
          {rows.map((r) => (
            <tr key={r.label}><td className="py-2 pr-3">{r.label}</td><td className="py-2 pr-3 text-right tabular-nums">{r.employeeCount}</td>
              <td className="py-2 pr-3 text-right tabular-nums">{money(r.totalGross)}</td><td className="py-2 pr-3 text-right tabular-nums text-rose-300">{money(r.totalDeduction)}</td>
              <td className="py-2 text-right tabular-nums text-emerald-300">{money(r.totalNet)}</td></tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Issues({ loader, link }) {
  if (loader.loading && !loader.data) return <Loading />;
  if (loader.error) return <ErrorText>{loader.error}</ErrorText>;
  const rows = loader.data || [];
  if (rows.length === 0) return <Empty>Không có.</Empty>;
  return (
    <ul className="divide-y divide-zinc-800 text-sm">
      {rows.map((i) => (
        <li key={i.userId} className="flex flex-wrap justify-between gap-2 py-2">
          <span>{link ? <Link className="text-amber-200 hover:underline" to={link(i)}>{i.fullName}</Link> : i.fullName} <span className="text-xs text-zinc-500">{i.employeeCode}{i.storeName ? ` · ${i.storeName}` : ""}</span></span>
          <span className="text-amber-300">{i.issue}</span>
        </li>
      ))}
    </ul>
  );
}
