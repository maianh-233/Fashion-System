import { Navigate } from "react-router-dom";
import { useAdminAuth } from "../../contexts/AdminAuthContext";

const ADMIN_ROLES = new Set(["ADMIN", "SUPER_ADMIN", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"]);

/** Bảo vệ giao diện nhạy cảm; backend vẫn là lớp kiểm soát quyền quyết định. */
export default function AdminOnlyRoute({ children }) {
  const { user } = useAdminAuth();
  const isAdmin = (user?.roles || []).some((role) =>
    ADMIN_ROLES.has(typeof role === "string" ? role : role?.code),
  );

  return isAdmin ? children : <Navigate to="/admin" replace />;
}
