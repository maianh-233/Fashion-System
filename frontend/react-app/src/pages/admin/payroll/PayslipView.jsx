import { Badge } from "../../../components/admin/recruitment/RecruitmentUi";
import { formatDate } from "../attendance/attendanceLogic";
import { KIND_LABEL, PAYMENT_STATUS, SOURCE_LABEL, money, num } from "./payrollLogic";

/** Chi tiết cách tính một phiếu lương (dùng cho HR và nhân viên). */
export default function PayslipView({ detail, actions }) {
  const r = detail.row;
  const income = detail.items.filter((i) => i.kind !== "DEDUCTION");
  const deductions = detail.items.filter((i) => i.kind === "DEDUCTION");
  const otHours = Number(detail.otWeekdayHours) + Number(detail.otWeekendHours) + Number(detail.otHolidayHours);
  return (
    <div className="space-y-4 text-sm">
      <div className="grid gap-2 sm:grid-cols-2">
        <Info label="Nhân viên" value={`${r.fullName} · ${r.employeeCode || "—"}`} />
        <Info label="Cửa hàng / phòng ban" value={`${r.storeName || "Văn phòng"}${r.departmentName ? ` · ${r.departmentName}` : ""}`} />
        <Info label="Chức vụ" value={r.positionName || "—"} />
        <Info label="Thời gian tính lương" value={`${formatDate(detail.windowStart)} – ${formatDate(detail.windowEnd)}`} />
      </div>

      <div>
        <h3 className="mb-2 font-semibold text-zinc-200">Ngày công</h3>
        <div className="grid grid-cols-3 gap-2 md:grid-cols-6">
          <Cell label="Công chuẩn" value={num(r.standardDays)} />
          <Cell label="Được trả lương" value={num(r.payableDays)} strong />
          <Cell label="Đi làm" value={num(r.workedDays)} />
          <Cell label="Nghỉ có lương" value={num(r.paidLeaveDays)} />
          <Cell label="Nghỉ không lương" value={num(Number(r.unpaidLeaveDays) + Number(r.confirmedAbsentDays))} />
          <Cell label="Ngày lễ" value={num(r.holidayDays)} />
        </div>
        <p className="mt-2 text-xs text-zinc-400">
          Đi trễ {detail.lateCount} lần ({detail.lateMinutes} phút) · Về sớm {detail.earlyCount} lần · Thiếu chấm công {detail.missingCheckOutCount + detail.missingCheckInCount} lần
          · Tăng ca đã duyệt {num(otHours)} giờ{Number(detail.offDayWorkedDays) > 0 && ` · Làm ngày nghỉ ${num(detail.offDayWorkedDays)}`}
          {Number(r.unverifiedDays) > 0 && <span className="text-amber-300"> · {num(r.unverifiedDays)} ngày chưa có dữ liệu chấm công</span>}
        </p>
      </div>

      <div>
        <h3 className="mb-2 font-semibold text-zinc-200">Thu nhập</h3>
        <Lines rows={[
          { name: "Lương cơ bản (hợp đồng)", note: "", amount: r.baseSalary, muted: true },
          { name: "Lương theo ngày công", note: `${num(r.basePay != null ? r.payableDays : 0)}/${num(r.standardDays)} ngày`, amount: r.basePay },
          ...income.map((i) => ({ name: `${KIND_LABEL[i.kind]}: ${i.name}`, note: [i.note, SOURCE_LABEL[i.source]].filter(Boolean).join(" · "), amount: i.amount, id: i.id, source: i.source })),
        ]} actions={actions} />
        <Total label="Tổng thu nhập" value={r.grossPay} />
      </div>
      <div>
        <h3 className="mb-2 font-semibold text-zinc-200">Khấu trừ</h3>
        {deductions.length === 0 ? <p className="text-zinc-500">Không có khoản khấu trừ.</p> : (
          <Lines rows={deductions.map((i) => ({ name: i.name, note: [i.note, SOURCE_LABEL[i.source]].filter(Boolean).join(" · "), amount: i.amount, id: i.id, source: i.source }))} actions={actions} />
        )}
        <Total label="Tổng khấu trừ" value={r.deductionTotal} />
      </div>
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3">
        <span className="text-base font-semibold">Thực nhận</span>
        <span className="flex items-center gap-3"><span className="text-xl font-bold tabular-nums text-emerald-300">{money(r.netPay)}</span><Badge map={PAYMENT_STATUS} value={r.paymentStatus} /></span>
      </div>
      {detail.payment && <p className="text-xs text-zinc-400">Đã thanh toán {formatDate(detail.payment.paidAt)} · {detail.payment.method === "CASH" ? "Tiền mặt" : "Chuyển khoản"}{detail.payment.reference && ` · ${detail.payment.reference}`}</p>}
    </div>
  );
}

function Lines({ rows, actions }) {
  return (
    <table className="min-w-full text-sm">
      <tbody className="divide-y divide-zinc-800">
        {rows.map((row, i) => (
          <tr key={row.id || i}>
            <td className={`py-1.5 pr-3 ${row.muted ? "text-zinc-500" : ""}`}>{row.name}{row.note && <div className="text-xs text-zinc-500">{row.note}</div>}</td>
            <td className={`py-1.5 text-right tabular-nums ${row.muted ? "text-zinc-500" : ""}`}>{money(row.amount)}</td>
            {actions && <td className="w-8 py-1.5 text-right">{row.id && actions(row)}</td>}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function Total({ label, value }) {
  return <div className="mt-1 flex justify-between border-t border-zinc-700 pt-1.5 font-semibold"><span>{label}</span><span className="tabular-nums">{money(value)}</span></div>;
}

function Info({ label, value }) {
  return <div><div className="text-xs text-zinc-500">{label}</div><div className="text-zinc-200">{value}</div></div>;
}

function Cell({ label, value, strong }) {
  return (
    <div className="rounded-xl border border-zinc-800 px-3 py-2">
      <div className="text-xs text-zinc-500">{label}</div>
      <div className={`tabular-nums ${strong ? "text-lg font-semibold text-amber-300" : "text-zinc-200"}`}>{value}</div>
    </div>
  );
}
