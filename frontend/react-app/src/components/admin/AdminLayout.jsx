import { Outlet } from "react-router-dom";
import AdminSidebar from "./AdminSidebar";
import AdminHeader from "./AdminHeader";
import useAdminNavigation from "../../hooks/useAdminNavigation";
import { AdminPermissionsProvider } from "../../contexts/AdminPermissionsContext";

function AdminLayoutContent() {
  const navigation = useAdminNavigation();

  return (
    <div className="admin-theme-root admin-shell flex h-screen min-h-screen flex-col overflow-hidden bg-zinc-950 text-zinc-200">
      <AdminHeader navigation={navigation} />

      <div className="admin-layout-body flex min-h-0 flex-1 flex-col md:flex-row">
        <AdminSidebar navigation={navigation} />
        <main className="admin-main min-w-0 flex-1 overflow-y-auto p-3 scrollbar-hide sm:p-4 lg:p-5">
          <div className="admin-content mx-auto w-full max-w-[1600px]">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}

export default function AdminLayout() {
  return <AdminPermissionsProvider><AdminLayoutContent /></AdminPermissionsProvider>;
}
