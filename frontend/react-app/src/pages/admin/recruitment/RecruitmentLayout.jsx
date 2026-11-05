import { NavLink, Outlet } from "react-router-dom";
import { BriefcaseBusiness, CalendarClock, ClipboardList, FileSignature, LayoutDashboard, ListTree, Mail, Megaphone, Users } from "lucide-react";
import AdminCatalogPageHeader from "../../../components/admin/common/AdminCatalogPageHeader";
import { ErrorText, RecruitmentLookupsProvider, useRecruitment } from "../../../components/admin/recruitment/RecruitmentUi";

const TABS = [
  { to: "/admin/recruitment", end: true, label: "Tổng quan", icon: LayoutDashboard },
  { to: "/admin/recruitment/campaigns", label: "Chiến dịch & vị trí", icon: Megaphone },
  { to: "/admin/recruitment/candidates", label: "Ứng viên", icon: Users },
  { to: "/admin/recruitment/applications", label: "Hồ sơ ứng tuyển", icon: ClipboardList },
  { to: "/admin/recruitment/interviews", label: "Lịch phỏng vấn", icon: CalendarClock },
  { to: "/admin/recruitment/offers", label: "Offer", icon: FileSignature, need: "offers" },
  { to: "/admin/recruitment/templates", label: "Mẫu quy trình", icon: ListTree, need: "manage" },
  { to: "/admin/recruitment/email-templates", label: "Mẫu email", icon: Mail, need: "manage" },
];

function Header() {
  const { scope, error, can } = useRecruitment();
  const tabs = TABS.filter((t) => !t.need || (t.need === "manage" ? can.manage : can.manage || can.approve));
  return (
    <>
      <AdminCatalogPageHeader icon={BriefcaseBusiness} eyebrow="Nhân sự" title="Tuyển dụng"
        description="Chiến dịch, vị trí, hồ sơ ứng viên, quy trình nhiều vòng, đánh giá, phỏng vấn và offer."
        status={<span className="rounded-full border border-zinc-700 px-3 py-1 text-xs text-zinc-300">
          {scope.storeId ? `Cửa hàng: ${scope.storeName}` : "Phạm vi: toàn chuỗi"}
        </span>} />
      <ErrorText>{error}</ErrorText>
      <nav className="flex gap-1 overflow-x-auto rounded-2xl border border-zinc-800 bg-zinc-900 p-1" aria-label="Tuyển dụng">
        {tabs.map(({ to, end, label, icon: Icon }) => (
          <NavLink key={to} to={to} end={end}
            className={({ isActive }) => `inline-flex shrink-0 items-center gap-2 rounded-xl px-3.5 py-2 text-sm ${isActive ? "bg-amber-400 font-semibold text-zinc-950" : "text-zinc-300 hover:bg-zinc-800 hover:text-white"}`}>
            <Icon size={15} />{label}
          </NavLink>
        ))}
      </nav>
    </>
  );
}

/** Khung chung của module tuyển dụng: tải danh mục + quyền một lần cho mọi trang con. */
export default function RecruitmentLayout() {
  return (
    <RecruitmentLookupsProvider>
      <div className="admin-catalog-page space-y-5 text-zinc-100">
        <Header />
        <Outlet />
      </div>
    </RecruitmentLookupsProvider>
  );
}
