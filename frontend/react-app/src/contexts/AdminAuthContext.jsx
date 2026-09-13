import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { logout as logoutRequest } from "../hooks/auth";
import {
  clearAdminSession,
  getAdminProfile,
  getAdminSession,
  saveAdminSession,
} from "../hooks/auth/adminSession";

const AdminAuthContext = createContext(null);

/** Khôi phục và quản lý duy nhất trạng thái xác thực của khu vực quản trị. */
export function AdminAuthProvider({ children }) {
  const [status, setStatus] = useState("initializing");
  const [user, setUser] = useState(null);

  const restoreSession = useCallback(async () => {
    setStatus("initializing");
    try {
      const profile = await getAdminProfile();
      setUser(profile);
      setStatus("authenticated");
      return true;
    } catch {
      setUser(null);
      setStatus("unauthenticated");
      return false;
    }
  }, []);

  useEffect(() => {
    // Access token không persist; request đầu tiên tự bootstrap từ HttpOnly refresh cookie.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    restoreSession();
  }, [restoreSession]);

  useEffect(() => {
    const handleSessionChange = () => {
      if (!getAdminSession()) {
        setUser(null);
        setStatus("unauthenticated");
      }
    };
    window.addEventListener("lunaria:admin-session", handleSessionChange);
    return () => {
      window.removeEventListener("lunaria:admin-session", handleSessionChange);
    };
  }, [restoreSession]);

  const establishSession = useCallback(async (authResponse) => {
    saveAdminSession(authResponse);
    setStatus("initializing");
    try {
      const profile = await getAdminProfile();
      setUser(profile);
      setStatus("authenticated");
      return profile;
    } catch (error) {
      setUser(null);
      setStatus("unauthenticated");
      throw error;
    }
  }, []);

  const logout = useCallback(async () => {
    const token = getAdminSession()?.token;
    try {
      await logoutRequest(token);
    } finally {
      clearAdminSession();
      setUser(null);
      setStatus("unauthenticated");
    }
  }, []);

  const syncUser = useCallback((nextUser) => {
    setUser(nextUser);
  }, []);

  const value = useMemo(() => ({
    status,
    user,
    establishSession,
    logout,
    restoreSession,
    syncUser,
  }), [establishSession, logout, restoreSession, status, syncUser, user]);

  return <AdminAuthContext.Provider value={value}>{children}</AdminAuthContext.Provider>;
}

/** Truy cập trạng thái và thao tác xác thực admin trong cây provider. */
// eslint-disable-next-line react-refresh/only-export-components
export function useAdminAuth() {
  const context = useContext(AdminAuthContext);
  if (!context) throw new Error("useAdminAuth phải được dùng bên trong AdminAuthProvider");
  return context;
}
