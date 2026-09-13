import { createContext, useContext } from "react";
import usePermissions from "../hooks/usePermissions";

const AdminPermissionsContext = createContext(null);

/** Nạp cây quyền hiệu lực đúng một lần cho toàn bộ khu vực quản trị. */
export function AdminPermissionsProvider({ children }) {
  const permissions = usePermissions();
  return <AdminPermissionsContext.Provider value={permissions}>{children}</AdminPermissionsContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAdminPermissions() {
  const context = useContext(AdminPermissionsContext);
  if (!context) throw new Error("useAdminPermissions phải được dùng trong AdminPermissionsProvider");
  return context;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useOptionalAdminPermissions() {
  return useContext(AdminPermissionsContext);
}
