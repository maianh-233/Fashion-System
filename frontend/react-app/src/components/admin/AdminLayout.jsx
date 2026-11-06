import { useEffect, useRef } from "react";
import { Outlet, useLocation } from "react-router-dom";
import AdminSidebar from "./AdminSidebar";
import AdminHeader from "./AdminHeader";
import AdminAssistant from "./assistant/AdminAssistant";
import useAdminNavigation from "../../hooks/useAdminNavigation";
import useSidebarState from "./useSidebarState";
import useResponsiveTables from "./useResponsiveTables";
import { AdminPermissionsProvider } from "../../contexts/AdminPermissionsContext";

function AdminLayoutContent() {
  const navigation = useAdminNavigation();
  const sidebar = useSidebarState();
  const mainRef = useRef(null);
  const { pathname } = useLocation();
  useResponsiveTables(mainRef);

  // Đổi trang thì về đầu trang (máy tính cuộn trong <main>, điện thoại cuộn cả trang).
  useEffect(() => {
    mainRef.current?.scrollTo({ top: 0 });
    window.scrollTo({ top: 0 });
    // Điện thoại: đưa tab / phân hệ đang chọn vào giữa thanh cuộn ngang để luôn nhìn thấy.
    if (!window.matchMedia?.("(max-width: 767px)").matches) return undefined;
    const timer = setTimeout(() => {
      document.querySelectorAll(".admin-header__navigation .is-active, .admin-content nav [aria-current='page']").forEach((el) => {
        const bar = el.closest("nav");
        if (!bar || bar.scrollWidth <= bar.clientWidth) return;
        const offset = el.getBoundingClientRect().left - bar.getBoundingClientRect().left + bar.scrollLeft;
        bar.scrollTo({ left: offset - (bar.clientWidth - el.offsetWidth) / 2, behavior: "smooth" });
      });
    }, 350);
    return () => clearTimeout(timer);
  }, [pathname]);

  return (
    <div className={`admin-theme-root admin-shell ${sidebar.collapsed ? "is-sidebar-collapsed" : ""} flex min-h-screen flex-col bg-zinc-950 text-zinc-200 md:h-screen md:overflow-hidden`}>
      <AdminHeader navigation={navigation} sidebarCollapsed={sidebar.collapsed} onToggleSidebar={sidebar.toggle} />

      <div className="admin-layout-body flex flex-1 flex-col md:min-h-0 md:flex-row">
        <AdminSidebar navigation={navigation} collapsed={sidebar.collapsed} onToggle={sidebar.toggle} />
        <main ref={mainRef} className="admin-main relative min-w-0 flex-1 p-3 scrollbar-hide sm:p-4 md:overflow-y-auto lg:p-5">
          <div className="admin-content mx-auto w-full max-w-[1600px]">
            <Outlet />
          </div>
        </main>
      </div>
      <AdminAssistant />
    </div>
  );
}

export default function AdminLayout() {
  return <AdminPermissionsProvider><AdminLayoutContent /></AdminPermissionsProvider>;
}
