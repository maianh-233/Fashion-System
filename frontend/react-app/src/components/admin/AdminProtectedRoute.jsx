import { Navigate, useLocation } from "react-router-dom";
import { useAdminAuth } from "../../contexts/AdminAuthContext";

/** Chặn toàn bộ cây route admin cho tới khi backend xác minh access token. */
export default function AdminProtectedRoute({ children }) {
  const { status } = useAdminAuth();
  const location = useLocation();

  if (status === "initializing") {
    return (
      <div className="admin-auth-loading" role="status" aria-live="polite">
        Đang xác minh phiên đăng nhập…
      </div>
    );
  }

  if (status !== "authenticated") {
    return (
      <Navigate
        to="/adminlogin"
        replace
        state={{ from: { pathname: location.pathname, search: location.search, hash: location.hash } }}
      />
    );
  }

  return children;
}
