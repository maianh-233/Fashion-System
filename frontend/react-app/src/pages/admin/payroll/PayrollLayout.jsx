/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { BarChart3, CalendarRange, LayoutDashboard, Settings, UserRoundCog, Wallet } from "lucide-react";
import AdminCatalogPageHeader from "../../../components/admin/common/AdminCatalogPageHeader";
import { ErrorText } from "../../../components/admin/recruitment/RecruitmentUi";
import { payrollApi } from "../../../api/admin/payrollApi";

const Ctx = createContext(null);

export function usePayroll() {
  const state = useContext(Ctx);
  const data = state?.data;
  return useMemo(() => ({
    loading: !data && !state?.error,
    can: data?.permissions || {},
    scopeStoreId: data?.scopeStoreId || null,
    scopeStoreName: data?.scopeStoreName || null,
    stores: data?.stores || [],
    components: data?.components || [],
    settings: data?.settings || null,
    reload: state?.reload,
  }), [data, state?.error, state?.reload]);
}

const TABS = [
  { to: "/admin/payroll", end: true, label: "Tổng quan", icon: LayoutDashboard, need: "view" },
  { to: "/admin/payroll/periods", label: "Kỳ lương", icon: CalendarRange, need: "view" },
  { to: "/admin/payroll/salaries", label: "Hồ sơ lương", icon: UserRoundCog, need: "salaryView" },
  { to: "/admin/payroll/reports", label: "Báo cáo", icon: BarChart3, need: "view" },
  { to: "/admin/payroll/settings", label: "Chính sách lương", icon: Settings, need: "view" },
];

export default function PayrollLayout() {
  const [version, setVersion] = useState(0);
  const [state, setState] = useState({ data: null, error: "" });
  useEffect(() => {
    const controller = new AbortController();
    payrollApi.lookups({ signal: controller.signal })
      .then((data) => setState({ data, error: "" }))
      .catch((e) => { if (!controller.signal.aborted) setState({ data: null, error: e.message }); });
    return () => controller.abort();
  }, [version]);
  const value = useMemo(() => ({ ...state, reload: () => setVersion((v) => v + 1) }), [state]);
  const can = state.data?.permissions || {};
  return (
    <Ctx.Provider value={value}>
      <div className="admin-catalog-page space-y-5 text-zinc-100 [&>*]:min-w-0">
        <AdminCatalogPageHeader icon={Wallet} eyebrow="Nhân sự" title="Tính lương"
          description="Lương cố định hằng tháng theo ngày công từ chấm công, phụ cấp, thưởng, tăng ca, khấu trừ; phê duyệt, chốt và thanh toán."
          status={<span className="rounded-full border border-zinc-700 px-3 py-1 text-xs text-zinc-300">
            {state.data?.scopeStoreId ? `Cửa hàng: ${state.data.scopeStoreName}` : "Phạm vi: toàn chuỗi"}
          </span>} />
        <ErrorText>{state.error}</ErrorText>
        <nav className="flex gap-1 overflow-x-auto rounded-2xl border border-zinc-800 bg-zinc-900 p-1" aria-label="Tính lương">
          {TABS.filter((t) => !state.data || can[t.need]).map(({ to, end, label, icon: Icon }) => (
            <NavLink key={to} to={to} end={end}
              className={({ isActive }) => `inline-flex shrink-0 items-center gap-2 rounded-xl px-3.5 py-2 text-sm ${isActive ? "bg-amber-400 font-semibold text-zinc-950" : "text-zinc-300 hover:bg-zinc-800 hover:text-white"}`}>
              <Icon size={15} />{label}
            </NavLink>
          ))}
        </nav>
        <Outlet />
      </div>
    </Ctx.Provider>
  );
}
