import { Navigate } from "react-router-dom";
import { useAdminPermissions } from "../../contexts/AdminPermissionsContext";

/** Chặn truy cập trực tiếp bằng URL nếu tài khoản không có quyền xem trang; truyền mảng nghĩa là có một trong các quyền. */
export default function AdminPermissionRoute({ permission, children, fallback = "/admin/profile" }) {
  const { loading, hasPermission } = useAdminPermissions();

  if (loading) return <div className="admin-auth-loading">Đang kiểm tra quyền truy cập…</div>;
  return [].concat(permission).some(hasPermission) ? children : <Navigate to={fallback} replace />;
}
