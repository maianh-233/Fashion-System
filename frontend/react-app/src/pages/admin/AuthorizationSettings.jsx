import { useEffect, useMemo, useState } from "react";
import {
  Boxes, CheckCircle2, KeyRound, Layers3, LoaderCircle, Pencil, Plus,
  RefreshCw, Search, Settings, ShieldCheck, Trash2,
} from "lucide-react";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import AdminDialog, {
  AdminDialogBody, AdminDialogFooter, AdminDialogHeader,
} from "../../components/admin/common/AdminDialog";
import { AdminField, AdminFormSection } from "../../components/admin/common/AdminForm";
import Button from "../../components/common/Button";
import { useSystemNotification } from "../../components/common/SystemNotification";
import { useAdminPermissions } from "../../contexts/AdminPermissionsContext";
import {
  createAuthorizationGroup, createAuthorizationModule, createAuthorizationPermission,
  deleteAuthorizationGroup, deleteAuthorizationModule, deleteAuthorizationPermission,
  getAuthorizationGroups, getAuthorizationModules, getAuthorizationPermissions,
  updateAuthorizationGroup, updateAuthorizationModule, updateAuthorizationPermission,
} from "../../api/auth/authorizationSettingsApi";

const resources = {
  modules: { label: "Module", singular: "module", icon: Boxes },
  groups: { label: "Nhóm quyền", singular: "nhóm quyền", icon: Layers3 },
  permissions: { label: "Permission", singular: "permission", icon: KeyRound },
};

const emptyForms = {
  modules: { code: "", name: "", description: "", icon: "boxes", sortOrder: 0, active: true },
  groups: { moduleId: "", code: "", name: "", description: "" },
  permissions: { groupId: "", code: "", name: "", description: "" },
};

const api = {
  modules: { create: createAuthorizationModule, update: updateAuthorizationModule, remove: deleteAuthorizationModule },
  groups: { create: createAuthorizationGroup, update: updateAuthorizationGroup, remove: deleteAuthorizationGroup },
  permissions: { create: createAuthorizationPermission, update: updateAuthorizationPermission, remove: deleteAuthorizationPermission },
};

function normalizeForm(type, record) {
  const base = { ...emptyForms[type], ...record };
  return { ...base, code: base.code || "", name: base.name || "", description: base.description || "" };
}

export default function AuthorizationSettings() {
  const notification = useSystemNotification();
  const { reload: reloadEffectivePermissions } = useAdminPermissions();
  const [activeType, setActiveType] = useState("modules");
  const [data, setData] = useState({ modules: [], groups: [], permissions: [] });
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [search, setSearch] = useState("");
  const [dialog, setDialog] = useState({ open: false, record: null });
  const [form, setForm] = useState(emptyForms.modules);
  const [saving, setSaving] = useState(false);

  const loadData = async () => {
    setLoading(true);
    setLoadError("");
    try {
      const [modules, groups, permissions] = await Promise.all([
        getAuthorizationModules(), getAuthorizationGroups(), getAuthorizationPermissions(),
      ]);
      setData({ modules, groups, permissions });
    } catch (error) {
      setLoadError(error.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;
    Promise.all([getAuthorizationModules(), getAuthorizationGroups(), getAuthorizationPermissions()])
      .then(([modules, groups, permissions]) => {
        if (active) setData({ modules, groups, permissions });
      })
      .catch((error) => active && setLoadError(error.message))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, []);

  const moduleById = useMemo(() => new Map(data.modules.map((item) => [item.id, item])), [data.modules]);
  const groupById = useMemo(() => new Map(data.groups.map((item) => [item.id, item])), [data.groups]);
  const visibleRows = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    if (!keyword) return data[activeType];
    return data[activeType].filter((item) =>
      [item.code, item.name, item.description].some((value) => String(value || "").toLowerCase().includes(keyword)),
    );
  }, [activeType, data, search]);

  const switchType = (type) => {
    setActiveType(type);
    setSearch("");
    setDialog({ open: false, record: null });
  };

  const openForm = (record = null) => {
    setForm(normalizeForm(activeType, record));
    setDialog({ open: true, record });
  };

  const updateField = (field, value) => setForm((current) => ({ ...current, [field]: value }));

  const handleSave = async (event) => {
    event.preventDefault();
    const code = form.code.trim().toUpperCase();
    const name = form.name.trim();
    if (!code || !name || !/^[A-Z][A-Z0-9_]*$/.test(code)) {
      notification.warning("Tên là bắt buộc và code chỉ được chứa chữ in hoa, số, dấu gạch dưới.");
      return;
    }
    if (activeType === "groups" && !form.moduleId) {
      notification.warning("Vui lòng chọn module cho nhóm quyền.");
      return;
    }
    if (activeType === "permissions" && !form.groupId) {
      notification.warning("Vui lòng chọn nhóm quyền cho permission.");
      return;
    }

    const payload = { ...form, code, name, description: form.description.trim() || null };
    delete payload.id;
    delete payload.createdAt;
    setSaving(true);
    try {
      if (dialog.record) await api[activeType].update(dialog.record.id, payload);
      else await api[activeType].create(payload);
      notification.success(`${dialog.record ? "Đã cập nhật" : "Đã tạo"} ${resources[activeType].singular}.`);
      setDialog({ open: false, record: null });
      await loadData();
      await reloadEffectivePermissions();
    } catch (error) {
      notification.error(error, { title: "Không thể lưu cấu hình" });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (record) => {
    const accepted = await notification.confirm({
      title: `Xóa ${resources[activeType].singular}`,
      message: `Bạn có chắc muốn xóa “${record.name}” (${record.code})?`,
      confirmText: "Xóa",
      destructive: true,
    });
    if (!accepted) return;
    try {
      await api[activeType].remove(record.id);
      notification.success(`Đã xóa ${resources[activeType].singular}.`);
      await loadData();
      await reloadEffectivePermissions();
    } catch (error) {
      notification.error(error, { title: "Không thể xóa cấu hình" });
    }
  };

  const currentResource = resources[activeType];

  return (
    <div className="admin-catalog-page authorization-settings-page">
      <AdminCatalogPageHeader
        icon={Settings}
        eyebrow="Thiết lập hệ thống"
        title="Quản lý Setting"
        description="Quản trị cây phân quyền Module → Nhóm quyền → Permission. Chỉ Admin và Super Admin được phép thao tác."
        status={<><ShieldCheck size={15} /> Khu vực dành cho Admin</>}
      />

      <section className="authorization-settings-stats">
        {Object.entries(resources).map(([type, resource]) => {
          const Icon = resource.icon;
          return <div key={type}><Icon size={18} /><span>{resource.label}</span><strong>{data[type].length}</strong></div>;
        })}
      </section>

      <nav className="authorization-settings-tabs" aria-label="Loại cấu hình">
        {Object.entries(resources).map(([type, resource]) => {
          const Icon = resource.icon;
          return <Button key={type} type="button" variant="unstyled" className={activeType === type ? "is-active" : ""} onClick={() => switchType(type)}><Icon size={16} /> {resource.label}</Button>;
        })}
      </nav>

      <section className="authorization-settings-card">
        <header className="authorization-settings-toolbar">
          <div className="authorization-settings-search"><Search size={16} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder={`Tìm ${currentResource.singular} theo tên hoặc code…`} /></div>
          <div>
            <Button type="button" variant="secondary" onClick={loadData} disabled={loading}><RefreshCw size={15} /> Làm mới</Button>
            <Button permission="SETTINGS_MANAGE" type="button" variant="primary" onClick={() => openForm()}><Plus size={15} /> Thêm {currentResource.singular}</Button>
          </div>
        </header>

        {loading ? (
          <div className="authorization-settings-state"><LoaderCircle className="animate-spin" /><span>Đang tải catalog phân quyền…</span></div>
        ) : loadError ? (
          <div className="authorization-settings-state is-error"><ShieldCheck /><span>{loadError}</span><Button variant="secondary" onClick={loadData}>Thử lại</Button></div>
        ) : (
          <div className="authorization-settings-table-wrap">
            <table className="authorization-settings-table">
              <thead><tr><th>Code</th><th>Tên hiển thị</th>{activeType !== "modules" && <th>Thuộc cấp</th>}{activeType === "modules" && <th>Thứ tự</th>}<th>Mô tả</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
              <tbody>
                {visibleRows.map((item) => {
                  const parent = activeType === "groups" ? moduleById.get(item.moduleId) : activeType === "permissions" ? groupById.get(item.groupId) : null;
                  return (
                    <tr key={item.id}>
                      <td><code>{item.code}</code></td>
                      <td><strong>{item.name}</strong></td>
                      {activeType !== "modules" && <td><span className="authorization-settings-parent">{parent?.name || "Không xác định"}<small>{parent?.code}</small></span></td>}
                      {activeType === "modules" && <td>{item.sortOrder ?? 0}</td>}
                      <td><p>{item.description || "—"}</p></td>
                      <td>{activeType === "modules" ? <span className={`authorization-settings-status ${item.active ? "is-active" : ""}`}><CheckCircle2 size={12} /> {item.active ? "Hoạt động" : "Tạm ẩn"}</span> : <span className="authorization-settings-status is-active"><CheckCircle2 size={12} /> Khả dụng</span>}</td>
                      <td><div className="authorization-settings-actions"><Button permission="SETTINGS_MANAGE" variant="unstyled" title="Chỉnh sửa" onClick={() => openForm(item)}><Pencil size={15} /></Button><Button permission="SETTINGS_MANAGE" variant="unstyled" title="Xóa" onClick={() => handleDelete(item)}><Trash2 size={15} /></Button></div></td>
                    </tr>
                  );
                })}
                {visibleRows.length === 0 && <tr><td className="authorization-settings-empty" colSpan={7}>Không tìm thấy dữ liệu phù hợp.</td></tr>}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <AdminDialog open={dialog.open} onClose={() => setDialog({ open: false, record: null })} size="md">
        <AdminDialogHeader title={`${dialog.record ? "Chỉnh sửa" : "Thêm"} ${currentResource.singular}`} description="Code được chuẩn hóa thành chữ in hoa và dùng làm định danh nghiệp vụ." onClose={() => setDialog({ open: false, record: null })} />
        <form onSubmit={handleSave}>
          <AdminDialogBody>
            <AdminFormSection title="Thông tin cấu hình" description="Các thay đổi sẽ tác động trực tiếp tới cây phân quyền của hệ thống.">
              <div className="authorization-settings-form-grid">
                {activeType === "groups" && <AdminField as="select" label="Module *" value={form.moduleId} onChange={(event) => updateField("moduleId", event.target.value)}><option value="">Chọn module</option>{data.modules.map((item) => <option key={item.id} value={item.id}>{item.name} · {item.code}</option>)}</AdminField>}
                {activeType === "permissions" && <AdminField as="select" label="Nhóm quyền *" value={form.groupId} onChange={(event) => updateField("groupId", event.target.value)}><option value="">Chọn nhóm quyền</option>{data.groups.map((item) => <option key={item.id} value={item.id}>{item.name} · {item.code}</option>)}</AdminField>}
                <AdminField label="Code *" value={form.code} maxLength={activeType === "permissions" ? 100 : 50} onChange={(event) => updateField("code", event.target.value.toUpperCase())} placeholder="VD: PRODUCT_VIEW" />
                <AdminField label="Tên hiển thị *" value={form.name} maxLength={activeType === "permissions" ? 150 : 100} onChange={(event) => updateField("name", event.target.value)} placeholder="Tên cấu hình" />
                {activeType === "modules" && <><AdminField label="Icon" value={form.icon || ""} maxLength={100} onChange={(event) => updateField("icon", event.target.value)} placeholder="settings" /><AdminField label="Thứ tự" type="number" min="0" value={form.sortOrder ?? 0} onChange={(event) => updateField("sortOrder", Number(event.target.value))} /><label className="authorization-settings-checkbox"><input type="checkbox" checked={Boolean(form.active)} onChange={(event) => updateField("active", event.target.checked)} /><span>Kích hoạt module</span></label></>}
                <AdminField as="textarea" rows="4" label="Mô tả" value={form.description} onChange={(event) => updateField("description", event.target.value)} containerClassName="authorization-settings-form-wide" />
              </div>
            </AdminFormSection>
          </AdminDialogBody>
          <AdminDialogFooter><Button type="button" variant="secondary" onClick={() => setDialog({ open: false, record: null })}>Hủy</Button><Button permission="SETTINGS_MANAGE" type="submit" variant="primary" loading={saving}>{dialog.record ? "Lưu thay đổi" : "Tạo mới"}</Button></AdminDialogFooter>
        </form>
      </AdminDialog>
    </div>
  );
}
