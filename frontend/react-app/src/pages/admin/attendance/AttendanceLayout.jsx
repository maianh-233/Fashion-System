/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { CalendarCheck, ClipboardCheck, Clock, LayoutDashboard, Settings } from "lucide-react";
import AdminCatalogPageHeader from "../../../components/admin/common/AdminCatalogPageHeader";
import { ErrorText } from "../../../components/admin/recruitment/RecruitmentUi";
import { attendanceApi } from "../../../api/admin/attendanceApi";

const LookupsContext = createContext(null);

/** Quyền, phạm vi cửa hàng, danh sách cửa hàng + loại nghỉ của người đang đăng nhập. */
export function useAttendance() {
  const state = useContext(LookupsContext);
  const data = state?.data;
  return useMemo(() => ({
    loading: !data && !state?.error,
    error: state?.error || "",
    can: data?.permissions || {},
    scopeStoreId: data?.scopeStoreId || null,
    scopeStoreName: data?.scopeStoreName || null,
    stores: data?.stores || [],
    leaveTypes: data?.leaveTypes || [],
    reload: state?.reload,
  }), [data, state?.error, state?.reload]);
}

const TABS = [
  { to: "/admin/attendance", end: true, label: "Tổng quan", icon: LayoutDashboard },
  { to: "/admin/attendance/records", label: "Bảng công", icon: CalendarCheck },
  { to: "/admin/attendance/requests", label: "Duyệt đơn", icon: ClipboardCheck },
  { to: "/admin/attendance/settings", label: "Cấu hình", icon: Settings },
];

export default function AttendanceLayout() {
  const [version, setVersion] = useState(0);
  const [state, setState] = useState({ data: null, error: "" });
  useEffect(() => {
    const controller = new AbortController();
    attendanceApi.lookups({ signal: controller.signal })
      .then((data) => setState({ data, error: "" }))
      .catch((e) => { if (!controller.signal.aborted) setState({ data: null, error: e.message }); });
    return () => controller.abort();
  }, [version]);
  const value = useMemo(() => ({ ...state, reload: () => setVersion((v) => v + 1) }), [state]);
  return (
    <LookupsContext.Provider value={value}>
      <div className="admin-catalog-page space-y-5 text-zinc-100 [&>*]:min-w-0">
        <AdminCatalogPageHeader icon={Clock} eyebrow="Nhân sự" title="Chấm công & nghỉ phép"
          description="Bảng công, đơn giải trình, nghỉ phép, tăng ca và cấu hình giờ làm. Dữ liệu là đầu vào cho bảng lương."
          status={<span className="rounded-full border border-zinc-700 px-3 py-1 text-xs text-zinc-300">
            {state.data?.scopeStoreId ? `Cửa hàng: ${state.data.scopeStoreName}` : "Phạm vi: toàn chuỗi"}
          </span>} />
        <ErrorText>{state.error}</ErrorText>
        <nav className="flex gap-1 overflow-x-auto rounded-2xl border border-zinc-800 bg-zinc-900 p-1" aria-label="Chấm công">
          {TABS.map(({ to, end, label, icon: Icon }) => (
            <NavLink key={to} to={to} end={end}
              className={({ isActive }) => `inline-flex shrink-0 items-center gap-2 rounded-xl px-3.5 py-2 text-sm ${isActive ? "bg-amber-400 font-semibold text-zinc-950" : "text-zinc-300 hover:bg-zinc-800 hover:text-white"}`}>
              <Icon size={15} />{label}
            </NavLink>
          ))}
        </nav>
        <Outlet />
      </div>
    </LookupsContext.Provider>
  );
}
