import { Outlet } from "react-router-dom";
import AdminSidebar from "./AdminSidebar";
import AdminHeader from "./AdminHeader";
import useAdminNavigation from "../../hooks/useAdminNavigation";

export default function AdminLayout() {
  const navigation = useAdminNavigation();

  return (
    <div className="admin-theme-root flex h-screen flex-col overflow-hidden bg-zinc-950 text-zinc-200">
      <AdminHeader navigation={navigation} />

      <div className="flex min-h-0 flex-1 flex-col md:flex-row">
        <AdminSidebar navigation={navigation} />
        <main className="min-w-0 flex-1 overflow-y-auto p-3 scrollbar-hide sm:p-4 lg:p-5">
          <div className="admin-content mx-auto w-full max-w-[1600px]">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
