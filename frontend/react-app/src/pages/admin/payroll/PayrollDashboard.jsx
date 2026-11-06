import { useState } from "react";
import { Link } from "react-router-dom";
import { payrollApi } from "../../../api/admin/payrollApi";
import { Badge, Empty, ErrorText, Field, Loading, Section, Stat, control, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import { currentMonth } from "../attendance/attendanceLogic";
import { usePayroll } from "./PayrollLayout";
import { PERIOD_STATUS, money, monthLabel } from "./payrollLogic";

export default function PayrollDashboard() {
  const { stores, scopeStoreId } = usePayroll();
  const [month, setMonth] = useState(currentMonth());
  const [storeId, setStoreId] = useState("");
  const { data, error, loading } = useLoader((signal) => payrollApi.dashboard({ month, storeId }, { signal }), [month, storeId]);

  return (
    <div className="space-y-5">
      <Section>
        <div className="grid gap-3 sm:grid-cols-3">
          <Field label="Tháng lương"><input type="month" className={control} value={month} onChange={(e) => setMonth(e.target.value || currentMonth())} /></Field>
          <Field label="Cửa hàng">
            <select className={control} value={storeId} onChange={(e) => setStoreId(e.target.value)} disabled={Boolean(scopeStoreId)}>
              {!scopeStoreId && <option value="">Toàn chuỗi</option>}
              {stores.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </Field>
        </div>
      </Section>
      <ErrorText>{error}</ErrorText>
      {loading && !data ? <Loading /> : data && (
        <>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <Stat label="Nhân viên được tính lương" value={data.employeeCount} />
            <Stat label="Tổng quỹ lương (thu nhập)" value={money(data.totalGross)} tone="amber" />
            <Stat label="Tổng thực nhận" value={money(data.totalNet)} tone="emerald" />
            <Stat label="Tổng khấu trừ" value={money(data.totalDeduction)} tone="rose" />
            <Stat label="Lương cơ bản (hợp đồng)" value={money(data.totalBaseSalary)} hint={`Theo công: ${money(data.totalBasePay)}`} />
            <Stat label="Tổng phụ cấp" value={money(data.totalAllowance)} tone="sky" />
            <Stat label="Tổng thưởng" value={money(data.totalBonus)} tone="violet" hint={`Tăng ca: ${money(data.totalOvertime)}`} />
            <Stat label="Bảng lương chờ duyệt / đã trả" value={`${data.pendingApproval} / ${data.paidPeriods}`} tone="amber" />
          </div>
          {(data.needsReview > 0 || data.missingSalaryCount > 0) && (
            <div className="rounded-2xl border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-100">
              {data.needsReview > 0 && <p>{data.needsReview} nhân viên trong tháng cần xem xét dữ liệu công.</p>}
              {data.missingSalaryCount > 0 && <p>{data.missingSalaryCount} nhân viên đang làm việc chưa có hồ sơ lương — <Link className="underline" to="/admin/payroll/salaries?filter=MISSING">xem danh sách</Link>.</p>}
            </div>
          )}
          <Section title={`Kỳ lương tháng ${monthLabel(data.month)}`} action={<Link to="/admin/payroll/periods" className="text-sm text-amber-300 hover:underline">Quản lý kỳ lương</Link>}>
            {data.periods.length === 0 ? <Empty>Chưa có kỳ lương nào trong tháng này.</Empty> : (
              <ul className="divide-y divide-zinc-800">
                {data.periods.map((p) => (
                  <li key={p.id}>
                    <Link to={`/admin/payroll/periods/${p.id}`} className="flex flex-wrap items-center justify-between gap-3 py-3 text-sm hover:text-amber-200">
                      <span><span className="font-medium">{p.storeName || "Văn phòng (Global)"}</span> <span className="text-xs text-zinc-500">{p.code}</span></span>
                      <span className="flex items-center gap-3"><span className="tabular-nums text-zinc-300">{p.employeeCount} NV · {money(p.totalNet)}</span><Badge map={PERIOD_STATUS} value={p.status} /></span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </Section>
        </>
      )}
    </div>
  );
}
