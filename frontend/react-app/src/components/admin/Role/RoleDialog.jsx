import Button from "../../common/Button";
import { useMemo, useState } from "react";
import { Info, ShieldCheck } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../common/AdminDialog";
import { AdminField, AdminFormSection } from "../common/AdminForm";
import GroupPermissionInfor from "./GroupPermissionInfor";

const emptyRole = () => ({ id: null, code: "", name: "", description: "", createdAt: null, permissionScopes: {} });

export default function RoleDialog({ open, mode = "view", role, permissionGroups = [], busy = false, error = "", onClose, onSave }) {
  const readOnly = mode === "view";
  const creating = mode === "create";
  const [activeTab, setActiveTab] = useState("role");
  const [form, setForm] = useState(() => creating ? emptyRole() : { ...emptyRole(), ...role, permissionScopes: { ...(role?.permissionScopes || {}) } });
  const [showValidation, setShowValidation] = useState(false);

  const permissionCount = Object.keys(form.permissionScopes).length;
  const initialSnapshot = useMemo(() => JSON.stringify(creating ? emptyRole() : { ...emptyRole(), ...role, permissionScopes: role?.permissionScopes || {} }), [creating, role]);
  const changed = JSON.stringify(form) !== initialSnapshot;
  const nameError = form.name.trim() ? "" : "Tên vai trò là bắt buộc.";
  const codeError = !form.code.trim()
    ? "Mã vai trò là bắt buộc."
    : /^[A-Z][A-Z0-9_]*$/.test(form.code) ? "" : "Chỉ dùng chữ in hoa, số và dấu gạch dưới.";

  const save = () => {
    setShowValidation(true);
    if (nameError || codeError) { setActiveTab("role"); return; }
    onSave?.({
      id: form.id,
      code: form.code.trim(),
      name: form.name.trim(),
      description: form.description.trim() || null,
      permissions: Object.entries(form.permissionScopes).map(([permissionId, scope]) => ({ permissionId, scope })),
    });
  };

  return (
    <AdminDialog open={open} onClose={busy ? undefined : onClose} size="full" className="rbac-dialog">
      <AdminDialogHeader
        title={creating ? "Tạo vai trò mới" : mode === "edit" ? "Chỉnh sửa vai trò" : "Chi tiết vai trò"}
        description="Dữ liệu được đọc và lưu trực tiếp trong hệ thống phân quyền"
        onClose={busy ? undefined : onClose}
      >
        <span className={`rbac-mode-badge rbac-mode-badge--${mode}`}>{readOnly ? "CHỈ ĐỌC" : creating ? "THÊM MỚI" : "ĐANG CHỈNH SỬA"}</span>
      </AdminDialogHeader>

      <nav className="rbac-tabs" aria-label="Các phần vai trò">
        <Button type="button" variant="unstyled" onClick={() => setActiveTab("role")} className={`rbac-tab ${activeTab === "role" ? "rbac-tab--active" : ""}`}><Info size={16} />Thông tin</Button>
        <Button type="button" variant="unstyled" onClick={() => setActiveTab("permissions")} className={`rbac-tab ${activeTab === "permissions" ? "rbac-tab--active" : ""}`}><ShieldCheck size={16} />Quyền theo role <small>{permissionCount}</small></Button>
      </nav>

      <AdminDialogBody>
        {error && <div className="mb-5 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</div>}
        {activeTab === "role" && (
          <AdminFormSection title="Thông tin vai trò" description="Mã vai trò được chuẩn hóa chữ in hoa và phải duy nhất.">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div><AdminField label="Tên vai trò *" value={form.name} disabled={readOnly} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} />{showValidation && nameError && <p className="rbac-field-error">{nameError}</p>}</div>
              <div><AdminField label="Mã vai trò *" value={form.code} disabled={readOnly || (!creating && Boolean(form.id))} onChange={(event) => setForm((current) => ({ ...current, code: event.target.value.toUpperCase() }))} />{showValidation && codeError && <p className="rbac-field-error">{codeError}</p>}</div>
              <AdminField as="textarea" rows="4" label="Mô tả" value={form.description || ""} disabled={readOnly} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} containerClassName="sm:col-span-2" />
              {!creating && <AdminField label="Thời điểm tạo" value={formatDate(form.createdAt)} disabled readOnly />}
            </div>
          </AdminFormSection>
        )}
        {activeTab === "permissions" && (
          <div className="space-y-4">
            {permissionGroups.length === 0 ? <div className="rbac-empty">Catalog quyền hiện chưa có dữ liệu.</div> : permissionGroups.map((group) => (
              <GroupPermissionInfor key={group.id} groupPermission={group} permissions={group.permissions} selectedPermissions={form.permissionScopes} mode={mode} onPermissionChange={(permissionScopes) => setForm((current) => ({ ...current, permissionScopes }))} />
            ))}
          </div>
        )}
      </AdminDialogBody>

      <AdminDialogFooter className="justify-end gap-3">
        <Button type="button" variant="secondary" onClick={onClose} disabled={busy} className="rounded-xl px-5 py-2">{readOnly ? "Đóng" : "Hủy"}</Button>
        {!readOnly && <Button type="button" variant="primary" onClick={save} disabled={busy || (!creating && !changed)} className="rounded-xl px-5 py-2">{busy ? "Đang lưu..." : creating ? "Tạo vai trò" : "Lưu thay đổi"}</Button>}
      </AdminDialogFooter>
    </AdminDialog>
  );
}

function formatDate(value) {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("vi-VN");
}
