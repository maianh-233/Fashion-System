import Button from "../common/Button";
import {
  ArrowLeftRight,
  BadgeCheck,
  Building2,
  Boxes,
  ChartColumn,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  ClipboardCheck,
  Folder,
  FolderTree,
  House,
  Layers3,
  Logs,
  Package2,
  PackageMinus,
  PackagePlus,
  ShieldCheck,
  Shirt,
  RotateCcw,
  ShoppingBag,
  Star,
  Settings,
  Tag,
  Tags,
  Truck,
  UserCog,
  UserPlus,
  Users,
  Warehouse,
  MessageCircle,
  Clock,
  Wallet,
  UserRound,
  ReceiptText,
  CalendarDays,
} from "lucide-react";
import { useNavigate } from "react-router-dom";

const groupIcons = {
  "building-2": Building2,
  "arrow-left-right": ArrowLeftRight,
  "badge-check": BadgeCheck,
  boxes: Boxes,
  "chart-column": ChartColumn,
  "clipboard-check": ClipboardCheck,
  "folder-tree": FolderTree,
  house: House,
  "layers-3": Layers3,
  logs: Logs,
  "message-circle": MessageCircle,
  "package-2": Package2,
  "package-minus": PackageMinus,
  "package-plus": PackagePlus,
  "shield-check": ShieldCheck,
  shirt: Shirt,
  "rotate-ccw": RotateCcw,
  "shopping-bag": ShoppingBag,
  star: Star,
  settings: Settings,
  tag: Tag,
  tags: Tags,
  truck: Truck,
  "user-cog": UserCog,
  "user-plus": UserPlus,
  users: Users,
  warehouse: Warehouse,
  clock: Clock,
  wallet: Wallet,
  "user-round": UserRound,
  "receipt-text": ReceiptText,
  "calendar-days": CalendarDays,
};

export default function AdminSidebar({ navigation, collapsed = false, onToggle }) {
  const navigate = useNavigate();
  const activeModule = navigation.activeModule;
  const groups = activeModule?.groups || [];
  const ModuleIcon = groupIcons[activeModule?.icon] || FolderTree;

  return (
    <aside id="admin-sidebar" className={`admin-sidebar ${collapsed ? "is-collapsed" : ""}`} aria-label="Menu chức năng">
      <div className="admin-sidebar__heading">
        <span className="admin-sidebar__module-icon"><ModuleIcon size={18} aria-hidden="true" /></span>
        <div>
          <p>Phân hệ đang chọn</p>
          <h2>{activeModule?.name || "Điều hướng"}</h2>
        </div>
      </div>

      <nav
        aria-label={activeModule ? `Nhóm quyền ${activeModule.name}` : "Nhóm quyền"}
        className="admin-sidebar__navigation scrollbar-hide"
      >
        <div className="admin-sidebar__section-label" aria-hidden="true">
          <span>Danh mục quản lý</span>
          <small>{groups.length.toString().padStart(2, "0")}</small>
        </div>

        {groups.map((group) => {
          const Icon = groupIcons[group.icon] || Folder;
          const isActive = navigation.activeGroupCode === group.code;

          return (
            <Button
              variant="unstyled"
              key={group.code}
              type="button"
              disabled={!group.path}
              onClick={() => group.path && navigate(group.path)}
              aria-current={isActive ? "page" : undefined}
              title={!group.path ? `${group.name} chưa được map tới page hiện có` : collapsed ? group.name : undefined}
              aria-label={collapsed ? group.name : undefined}
              className={`admin-sidebar__item ${isActive ? "is-active" : ""}`}
            >
              <span className="admin-sidebar__item-icon"><Icon size={16} /></span>
              <span className="admin-sidebar__item-label">{group.name}</span>
              <ChevronRight className="admin-sidebar__item-arrow" size={14} aria-hidden="true" />
            </Button>
          );
        })}

        {!navigation.loading && activeModule && groups.length === 0 && (
          <p className="admin-sidebar__empty">
            Module này chưa có nhóm quyền khả dụng.
          </p>
        )}
      </nav>

      <footer className="admin-sidebar__footer">
        <span className="admin-sidebar__status-dot" aria-hidden="true" />
        <div className="admin-sidebar__footer-text">
          <strong>Hệ thống ổn định</strong>
          <small>Lunaria Admin · v1.0</small>
        </div>
        {onToggle && (
          <Button
            variant="unstyled"
            type="button"
            className="admin-sidebar__collapse"
            onClick={onToggle}
            aria-controls="admin-sidebar"
            aria-expanded={!collapsed}
            aria-label={collapsed ? "Mở rộng menu" : "Thu gọn menu"}
            title={`${collapsed ? "Mở rộng menu" : "Thu gọn menu"} (Ctrl + B)`}
          >
            {collapsed ? <ChevronsRight size={16} aria-hidden="true" /> : <ChevronsLeft size={16} aria-hidden="true" />}
          </Button>
        )}
      </footer>
    </aside>
  );
}
