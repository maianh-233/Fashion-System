import Button from "../../common/Button";
import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, ArrowRight, Info, KeyRound, LayoutDashboard, ScrollText, ShieldCheck, UsersRound } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../common/AdminDialog";
import { AdminField, AdminFormSection } from "../common/AdminForm";
import GroupPermissionInfor from "./GroupPermissionInfor";
import RoleUserAssignments from "./RoleUserAssignments";
import DirectUserPermissions from "./DirectUserPermissions";
import RolePermissionAudit from "./RolePermissionAudit";
import RoleOverview from "./RoleOverview";

const emptyRole = { id: null, code: "", name: "", description: "", created_at: "" };
const editTabs = [
  { id: "role", label: "Thông tin", icon: Info },
  { id: "users", label: "Người dùng", icon: UsersRound },
  { id: "permissions", label: "Quyền theo role", icon: ShieldCheck },
  { id: "direct", label: "Quyền trực tiếp", icon: KeyRound },
  { id: "audit", label: "Lịch sử", icon: ScrollText },
];

const makeSnapshot = (form, userIds, permissionIds, direct) => JSON.stringify({ form, userIds, permissionIds, direct });

export default function RoleDialog({ open, mode = "view", role, users = [], permissionGroups = [], auditEntries = [], onClose, onSave }) {
  const readOnly = mode === "view";
  const isCreate = mode === "create";
  const [activeTab, setActiveTab] = useState(readOnly ? "overview" : "role");
  const [formData, setFormData] = useState(emptyRole);
  const [selectedUserIds, setSelectedUserIds] = useState([]);
  const [selectedPermissionIds, setSelectedPermissionIds] = useState([]);
  const [directPermissions, setDirectPermissions] = useState({});
  const [directUserId, setDirectUserId] = useState("");
  const [initialSnapshot, setInitialSnapshot] = useState("");
  const [showValidation, setShowValidation] = useState(false);
  const allPermissions = useMemo(() => permissionGroups.flatMap((group) => group.permissions || []), [permissionGroups]);
  const entries = auditEntries.length ? auditEntries : role?.audit_entries || [];
  const tabs = useMemo(() => {
    if (readOnly) return [{ id: "overview", label: "Tổng quan", icon: LayoutDashboard }, ...editTabs.slice(1)];
    return isCreate ? editTabs.filter((tab) => tab.id !== "audit") : editTabs;
  }, [isCreate, readOnly]);

  useEffect(() => {
    if (!open) return;
    const nextForm = isCreate ? emptyRole : { ...emptyRole, ...role };
    const nextUsers = isCreate ? [] : role?.user_ids || [];
    const nextPermissions = isCreate ? [] : role?.permission_ids || role?.permissions?.map((item) => item.id || item) || [];
    const nextDirect = isCreate ? {} : role?.direct_user_permissions || {};
    setActiveTab(readOnly ? "overview" : "role");
    setFormData(nextForm);
    setSelectedUserIds(nextUsers);
    setSelectedPermissionIds(nextPermissions);
    setDirectPermissions(nextDirect);
    setDirectUserId("");
    setShowValidation(false);
    setInitialSnapshot(makeSnapshot(nextForm, nextUsers, nextPermissions, nextDirect));
  }, [open, isCreate, readOnly, role]);

  const currentSnapshot = makeSnapshot(formData, selectedUserIds, selectedPermissionIds, directPermissions);
  const hasChanges = currentSnapshot !== initialSnapshot;
  const nameError = !formData.name.trim() ? "Tên vai trò là bắt buộc." : "";
  const codeError = !formData.code.trim()
    ? "Mã vai trò là bắt buộc."
    : !/^[A-Z][A-Z0-9_]*$/.test(formData.code)
      ? "Chỉ dùng chữ in hoa, số và dấu gạch dưới."
      : "";
  const isValid = !nameError && !codeError;
  const currentIndex = tabs.findIndex((tab) => tab.id === activeTab);
  const isLastCreateStep = isCreate && currentIndex === tabs.length - 1;

  const handleUsersChange = (ids) => {
    setSelectedUserIds(ids);
    if (directUserId && !ids.includes(directUserId)) setDirectUserId("");
    setDirectPermissions((current) => Object.fromEntries(Object.entries(current).filter(([userId]) => ids.includes(userId))));
  };

  const handleSave = () => {
    setShowValidation(true);
    if (!isValid) { setActiveTab("role"); return; }
    onSave?.({ ...formData, user_ids: selectedUserIds, permission_ids: selectedPermissionIds, permissions: selectedPermissionIds, direct_user_permissions: directPermissions });
  };

  const goNext = () => {
    if (activeTab === "role" && !isValid) { setShowValidation(true); return; }
    setActiveTab(tabs[Math.min(currentIndex + 1, tabs.length - 1)].id);
  };

  const title = isCreate ? "Tạo vai trò mới" : mode === "edit" ? "Chỉnh sửa phân quyền" : "Chi tiết phân quyền";
  const modeLabel = isCreate ? "THÊM MỚI" : mode === "edit" ? "ĐANG CHỈNH SỬA" : "CHỈ ĐỌC";

  return (
    <AdminDialog open={open} onClose={onClose} size="full" className="rbac-dialog">
      <AdminDialogHeader title={title} description="Quản lý RBAC theo cấu trúc auth_db" onClose={onClose}>
        <span className={`rbac-mode-badge rbac-mode-badge--${mode}`}>{modeLabel}</span>
      </AdminDialogHeader>
      <nav className="rbac-tabs" aria-label="Các phần phân quyền">
        {tabs.map(({ id, label, icon: Icon }, index) => (
          <Button key={id} type="button" variant="unstyled" onClick={() => setActiveTab(id)} aria-current={activeTab === id ? "step" : undefined} className={`rbac-tab ${activeTab === id ? "rbac-tab--active" : ""}`}>
            {isCreate && <small>{index + 1}</small>}<Icon size={16} /><span>{label}</span>
          </Button>
        ))}
      </nav>
      <AdminDialogBody>
        {activeTab === "overview" && <RoleOverview role={formData} users={users} userIds={selectedUserIds} permissionIds={selectedPermissionIds} directPermissions={directPermissions} auditEntries={entries} />}
        {activeTab === "role" && (
          <AdminFormSection title="Thông tin vai trò" description="Các trường được lưu trong bảng roles.">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div><AdminField label="Tên vai trò *" value={formData.name} disabled={readOnly} onChange={(e) => setFormData({ ...formData, name: e.target.value })} placeholder="Quản trị viên" />{showValidation && nameError && <p className="rbac-field-error">{nameError}</p>}</div>
              <div><AdminField label="Mã vai trò *" value={formData.code} disabled={readOnly || (!isCreate && Boolean(formData.id))} onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })} placeholder="ADMIN" className="uppercase" />{showValidation && codeError && <p className="rbac-field-error">{codeError}</p>}</div>
              <AdminField as="textarea" rows="4" label="Mô tả" value={formData.description} disabled={readOnly} onChange={(e) => setFormData({ ...formData, description: e.target.value })} containerClassName="sm:col-span-2" />
              {!isCreate && <AdminField label="Thời điểm tạo" value={formData.created_at || ""} disabled readOnly />}
            </div>
          </AdminFormSection>
        )}
        {activeTab === "users" && <RoleUserAssignments users={users} selectedUserIds={selectedUserIds} readOnly={readOnly} onChange={handleUsersChange} />}
        {activeTab === "permissions" && <div className="space-y-4">{permissionGroups.map((group) => <GroupPermissionInfor key={group.id} groupPermission={group} permissions={group.permissions} selectedPermissionIds={selectedPermissionIds} mode={mode} onPermissionChange={setSelectedPermissionIds} />)}</div>}
        {activeTab === "direct" && <DirectUserPermissions users={users} assignedUserIds={selectedUserIds} permissionGroups={permissionGroups} directPermissions={directPermissions} selectedUserId={directUserId} onSelectedUserChange={setDirectUserId} onChange={setDirectPermissions} readOnly={readOnly} />}
        {activeTab === "audit" && <RolePermissionAudit entries={entries} permissions={allPermissions} />}
      </AdminDialogBody>
      <AdminDialogFooter className="admin-dialog__footer--split">
        <div className="rbac-footer-note">{mode === "edit" ? (hasChanges ? "Có thay đổi chưa lưu" : "Chưa có thay đổi") : isCreate ? `Bước ${currentIndex + 1}/${tabs.length}` : "Dữ liệu chỉ đọc"}</div>
        <div className="flex gap-2">
          {isCreate && currentIndex > 0 && <Button type="button" variant="secondary" onClick={() => setActiveTab(tabs[currentIndex - 1].id)} className="rounded-xl px-4 py-2"><ArrowLeft size={16} /> Quay lại</Button>}
          <Button type="button" variant="secondary" onClick={onClose} className="rounded-xl px-5 py-2">{readOnly ? "Đóng" : "Hủy"}</Button>
          {isCreate && !isLastCreateStep && <Button type="button" variant="primary" onClick={goNext} className="rounded-xl px-5 py-2">Tiếp tục <ArrowRight size={16} /></Button>}
          {isCreate && isLastCreateStep && <Button type="button" variant="primary" onClick={handleSave} className="rounded-xl px-5 py-2">Tạo vai trò</Button>}
          {mode === "edit" && <Button type="button" variant="primary" onClick={handleSave} disabled={!hasChanges} className="rounded-xl px-5 py-2">Lưu thay đổi</Button>}
        </div>
      </AdminDialogFooter>
    </AdminDialog>
  );
}
