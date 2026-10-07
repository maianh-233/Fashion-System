import Button from "../common/Button";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Boxes,
  ChartColumn,
  ChevronDown,
  House,
  LogOut,
  Settings,
  Shirt,
  ShoppingBag,
  Sparkles,
  Users,
  Warehouse,
} from "lucide-react";
import ThemeToggle from "../common/ThemeToggle";
import { useSystemNotification } from "../common/SystemNotification";
import { getModuleLandingPath } from "./adminNavigation";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import { getAdminSession } from "../../api/auth/adminSession";
import NotificationBell from "../../features/notifications/NotificationBell";

const moduleIcons = {
  boxes: Boxes,
  "chart-column": ChartColumn,
  house: House,
  settings: Settings,
  shirt: Shirt,
  "shopping-bag": ShoppingBag,
  users: Users,
  warehouse: Warehouse,
};

export default function AdminHeader({ navigation }) {
  const [sessionUser, setSessionUser] = useState(() => getAdminSession()?.user || null);
  const [loggingOut, setLoggingOut] = useState(false);
  const { logout } = useAdminAuth();
  const systemNotification = useSystemNotification();
  const navigate = useNavigate();

  const activeModule = navigation.modules.find(
    (module) => module.code === navigation.activeModuleCode,
  );

  useEffect(() => {
    const syncSession = () => setSessionUser(getAdminSession()?.user || null);
    window.addEventListener("lunaria:admin-session", syncSession);
    window.addEventListener("storage", syncSession);
    return () => {
      window.removeEventListener("lunaria:admin-session", syncSession);
      window.removeEventListener("storage", syncSession);
    };
  }, []);

  const displayName = sessionUser?.username || "Tài khoản quản trị";
  const displayRole = sessionUser?.roles?.[0] || "Nội bộ";
  const profileInitials = displayName.split(/\s+/).slice(-2).map((part) => part[0]).join("").toUpperCase();

  const handleLogout = async () => {
    if (loggingOut) return;
    setLoggingOut(true);
    try {
      await logout();
    } catch {
      systemNotification.warning("Không thể kết nối API đăng xuất, phiên trên thiết bị vẫn đã được xóa.");
    } finally {
      navigate("/adminlogin", { replace: true });
    }
  };

  return (
    <header className="admin-header">
      <div className="admin-header__brand">
        <span className="admin-header__brand-mark"><Sparkles size={19} /></span>
        <div>
          <h1>LUNARIA</h1>
          <p>Administration</p>
        </div>
      </div>

      <nav
        aria-label="Module quản trị"
        className="admin-header__navigation scrollbar-hide"
      >
        {navigation.modules.map((module) => {
          const Icon = moduleIcons[module.icon] || Boxes;
          const isActive = navigation.activeModuleCode === module.code;
          const landingPath = getModuleLandingPath(module);

          return (
            <Button
              variant="unstyled"
              key={module.code}
              type="button"
              disabled={!landingPath}
              onClick={() => landingPath && navigate(landingPath)}
              aria-current={isActive ? "page" : undefined}
              className={`admin-header__nav-item ${
                isActive
                  ? "is-active"
                  : ""
              }`}
            >
              <Icon size={17} />
              <span>{module.name}</span>
            </Button>
          );
        })}
      </nav>

      <div className="admin-header__actions">
        <span className="admin-header__context">{activeModule?.name || "Tổng quan"}</span>
        <ThemeToggle />
        <NotificationBell />

        <Button
          type="button"
          variant="unstyled"
          className="admin-header__profile"
          onClick={() => navigate("/admin/profile")}
        >
          <div className="admin-header__profile-avatar" aria-hidden="true">{profileInitials}</div>
          <span>
            <strong>{displayName}</strong>
            <small>{displayRole}</small>
          </span>
          <ChevronDown size={14} aria-hidden="true" />
        </Button>
        <Button
          type="button"
          variant="unstyled"
          className="admin-header__icon-button"
          loading={loggingOut}
          onClick={handleLogout}
          aria-label="Đăng xuất khỏi hệ thống"
          title="Đăng xuất"
        >
          {!loggingOut && <LogOut size={18} aria-hidden="true" />}
        </Button>
      </div>
    </header>
  );
}
