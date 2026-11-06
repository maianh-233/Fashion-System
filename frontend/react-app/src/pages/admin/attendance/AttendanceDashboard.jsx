import { useState } from "react";
import { Link } from "react-router-dom";
import { attendanceApi } from "../../../api/admin/attendanceApi";
import { ErrorText, Field, Loading, Section, Stat, control, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import { useAttendance } from "./AttendanceLayout";
import { formatDate, isoDate } from "./attendanceLogic";

export default function AttendanceDashboard() {
  const { stores, scopeStoreId } = useAttendance();
  const [date, setDate] = useState(isoDate());
  const [storeId, setStoreId] = useState("");
  const { data, error, loading } = useLoader((signal) => attendanceApi.dashboard({ date, storeId }, { signal }), [date, storeId]);

  return (
    <div className="space-y-5">
      <Section>
        <div className="grid gap-3 sm:grid-cols-3">
          <Field label="Ngày">
            <input type="date" className={control} value={date} max={isoDate()} onChange={(e) => setDate(e.target.value || isoDate())} />
          </Field>
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
          <div>
            <h2 className="mb-3 text-sm font-semibold text-zinc-300">Tình hình ngày {formatDate(data.date)}</h2>
            <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
              <Stat label="Tổng nhân viên" value={data.totalEmployees} />
              <Stat label="Đã check-in" value={data.checkedIn} tone="emerald" />
              <Stat label="Chưa check-in" value={data.notCheckedIn} tone="rose" hint="Không tính người nghỉ phép" />
              <Stat label="Đang nghỉ phép" value={data.onLeave} tone="violet" />
              <Stat label="Đi trễ" value={data.late} tone="amber" />
              <Stat label="Về sớm" value={data.earlyLeave} tone="amber" />
              <Stat label="Đã check-out" value={data.checkedOut} tone="sky" />
              <Stat label="Thiếu check-out" value={data.missingCheckOut} tone="rose" hint="Đã quá giờ tan ca" />
            </div>
          </div>
          {data.scheduledShifts > 0 && (
            <div>
              <h2 className="mb-3 text-sm font-semibold text-zinc-300">Theo lịch xếp ca</h2>
              <div className="grid grid-cols-2 gap-3 md:grid-cols-3">
                <Link to="/admin/attendance/schedule" className="block rounded-2xl transition hover:ring-1 hover:ring-amber-400/50">
                  <Stat label="Ca đã công bố" value={data.scheduledShifts} tone="sky" hint="Xem lịch xếp ca" />
                </Link>
                <Stat label="Đã vào ca, chưa check-in" value={data.notYetCheckedIn} tone={data.notYetCheckedIn ? "amber" : "zinc"} />
                <Stat label="Vắng ca" value={data.absentShifts} tone={data.absentShifts ? "rose" : "zinc"} hint="Hết ca mà không chấm công, không nghỉ phép" />
              </div>
            </div>
          )}
          <div>
            <h2 className="mb-3 text-sm font-semibold text-zinc-300">Đơn chờ duyệt</h2>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
              {[["excuses", "Giải trình", data.pendingExcuses], ["leaves", "Nghỉ phép", data.pendingLeaves], ["overtime", "Tăng ca", data.pendingOvertime]].map(([kind, label, value]) => (
                <Link key={kind} to={`/admin/attendance/requests?kind=${kind}`} className="block rounded-2xl transition hover:ring-1 hover:ring-amber-400/50">
                  <Stat label={label} value={value} tone={value ? "amber" : "zinc"} hint="Bấm để xử lý" />
                </Link>
              ))}
              <Link to="/admin/attendance/swaps" className="block rounded-2xl transition hover:ring-1 hover:ring-amber-400/50">
                <Stat label="Đổi ca" value={data.pendingSwaps} tone={data.pendingSwaps ? "amber" : "zinc"} hint="Bấm để xử lý" />
              </Link>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
