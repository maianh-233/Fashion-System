import Button from "../common/Button";
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Bell,
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
import { getAdminSession } from "../../hooks/auth/adminSession";

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
  const [showNotifications, setShowNotifications] = useState(false);
  const [sessionUser, setSessionUser] = useState(() => getAdminSession()?.user || null);
  const [loggingOut, setLoggingOut] = useState(false);
  const notificationRef = useRef(null);
  const { logout } = useAdminAuth();
  const systemNotification = useSystemNotification();
  const navigate = useNavigate();

  const notifications = [
    {
      id: 1,
      title: "Đơn hàng #LN2405131 vừa thanh toán",
      time: "2 phút trước",
      unread: true,
    },
    {
      id: 2,
      title: "Sản phẩm Áo khoác Denim sắp hết hàng",
      time: "8 phút trước",
      unread: true,
    },
    {
      id: 3,
      title: "Khách hàng mới: Trần Minh Khoa",
      time: "15 phút trước",
      unread: true,
    },
    {
      id: 4,
      title: "Phiếu nhập #PN240522 đã được duyệt",
      time: "25 phút trước",
      unread: false,
    },
    {
      id: 5,
      title: "Báo cáo kho ngày 21/05 đã sẵn sàng",
      time: "40 phút trước",
      unread: false,
    },
  ];

  const unreadCount = notifications.filter((item) => item.unread).length;
  const activeModule = navigation.modules.find(
    (module) => module.code === navigation.activeModuleCode,
  );

  useEffect(() => {
    function handleClickOutside(event) {
      if (
        notificationRef.current &&
        !notificationRef.current.contains(event.target)
      ) {
        setShowNotifications(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

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
        <div
          className="admin-header__notification"
          ref={notificationRef}
        >
          <Button
            variant="unstyled"
            onClick={() => setShowNotifications((prev) => !prev)}
            className="admin-header__icon-button"
            aria-label="Mở thông báo"
            aria-expanded={showNotifications}
          >
            <Bell size={18} />

            <span className="admin-header__notification-count">
              {unreadCount}
            </span>
          </Button>

          {showNotifications && (
            <div className="admin-header__notification-panel bg-zinc-900 border border-zinc-700">
              <div className="px-4 py-3 border-b border-zinc-800">
                <p className="font-semibold text-white">
                  Thông báo mới
                </p>
              </div>

              <div className="max-h-72 overflow-y-auto [scrollbar-width:none] [-ms-overflow-style:none] [&::-webkit-scrollbar]:hidden">
                {notifications.map((item) => (
                  <Button
                    key={item.id}
                    className="w-full text-left px-4 py-3 border-b border-zinc-800/70 hover:bg-zinc-800/70 transition-colors"
                  >
                    <div className="flex items-start gap-3">
                      <span
                        className={`mt-1 w-2 h-2 rounded-full ${
                          item.unread
                            ? "bg-emerald-400"
                            : "bg-zinc-600"
                        }`}
                      />

                      <div>
                        <p className="text-sm text-zinc-100">
                          {item.title}
                        </p>

                        <p className="text-xs text-zinc-400 mt-1">
                          {item.time}
                        </p>
                      </div>
                    </div>
                  </Button>
                ))}
              </div>

              <div className="p-3">
                <Button className="w-full rounded-xl py-2 bg-zinc-800 hover:bg-zinc-700 text-amber-400 font-medium transition-colors">
                  Xem thêm
                </Button>
              </div>
            </div>
          )}
        </div>

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
