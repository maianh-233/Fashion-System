import { Outlet } from "react-router-dom";
import AdminSidebar from "./AdminSidebar";
import AdminHeader from "./AdminHeader";

export default function AdminLayout() {
  return (
    <div className="flex h-screen overflow-hidden bg-zinc-950 text-zinc-200">
      <AdminSidebar />

      <div className="flex-1 flex flex-col">
        <AdminHeader />
        <div className="flex-1 overflow-y-auto scrollbar-hide p-8">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
