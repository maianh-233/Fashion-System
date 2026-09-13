import Button from "../common/Button";
import {
  BadgeCheck,
  Building2,
  Boxes,
  ChartColumn,
  ChevronRight,
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
  ShoppingBag,
  Settings,
  Tag,
  Tags,
  Truck,
  UserCog,
  Users,
  Warehouse,
} from "lucide-react";
import { useNavigate } from "react-router-dom";

const groupIcons = {
  "building-2": Building2,
  "badge-check": BadgeCheck,
  boxes: Boxes,
  "chart-column": ChartColumn,
  "clipboard-check": ClipboardCheck,
  "folder-tree": FolderTree,
  house: House,
  "layers-3": Layers3,
  logs: Logs,
  "package-2": Package2,
  "package-minus": PackageMinus,
  "package-plus": PackagePlus,
  "shield-check": ShieldCheck,
  shirt: Shirt,
  "shopping-bag": ShoppingBag,
  settings: Settings,
  tag: Tag,
  tags: Tags,
  truck: Truck,
  "user-cog": UserCog,
  users: Users,
  warehouse: Warehouse,
};

export default function AdminSidebar({ navigation }) {
  const navigate = useNavigate();
  const activeModule = navigation.activeModule;
  const groups = activeModule?.groups || [];
  const ModuleIcon = groupIcons[activeModule?.icon] || FolderTree;

  return (
    <aside className="admin-sidebar">
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
              title={!group.path ? `${group.name} chưa được map tới page hiện có` : undefined}
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
        <div>
          <strong>Hệ thống ổn định</strong>
          <small>Lunaria Admin · v1.0</small>
        </div>
      </footer>
    </aside>
  );
}
