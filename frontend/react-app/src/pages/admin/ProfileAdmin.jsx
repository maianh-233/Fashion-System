import { useMemo, useState } from "react";
import {
  BadgeCheck, BriefcaseBusiness, Building2, CalendarDays, CheckCircle2,
  Clock3, KeyRound, LoaderCircle, LockKeyhole, Mail, MapPin, Phone,
  Save, ShieldCheck, UserRound,
} from "lucide-react";
import Button from "../../components/common/Button";
import AdminDetailDialog from "../../components/admin/common/AdminDetailDialog";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import { useAdminPermissions } from "../../contexts/AdminPermissionsContext";
import {
  changeAdminPassword, updateAdminProfile,
} from "../../api/auth/adminSession";

const employmentStatusLabels = {
  ACTIVE: "Đang làm việc", PROBATION: "Thử việc", ON_LEAVE: "Đang nghỉ phép",
  SUSPENDED: "Tạm đình chỉ", TERMINATED: "Đã nghỉ việc",
};
const employmentTypeLabels = {
  FULL_TIME: "Toàn thời gian", PART_TIME: "Bán thời gian", CONTRACT: "Hợp đồng",
  INTERN: "Thực tập", TEMPORARY: "Thời vụ",
};

function formatDate(value, includeTime = false) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("vi-VN", includeTime
    ? { dateStyle: "short", timeStyle: "short" }
    : { dateStyle: "short" }).format(date);
}

function initials(name) {
  return (name || "Admin").trim().split(/\s+/).slice(-2).map((part) => part[0]).join("").toUpperCase();
}

export default function ProfileAdmin() {
  const { user: authenticatedUser, syncUser } = useAdminAuth();
  const [tab, setTab] = useState("profile");
  const [updatedProfile, setUpdatedProfile] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState(() => ({
    phone: authenticatedUser?.phone || "",
    dateOfBirth: authenticatedUser?.dateOfBirth || "",
    gender: authenticatedUser?.gender || "",
    avatar: authenticatedUser?.avatar || "",
  }));
  const [passwords, setPasswords] = useState({ currentPassword: "", newPassword: "", confirmPassword: "" });
  const [modal, setModal] = useState({ open: false, title: "", message: "" });
  const {
    modules: permissionModules,
    loading: permissionsLoading,
    error: permissionsError,
    reload: reloadPermissions,
  } = useAdminPermissions();

  const profile = updatedProfile || authenticatedUser;
  const displayProfile = useMemo(() => profile || {}, [profile]);
  const permissionCount = useMemo(() => permissionModules.reduce(
    (moduleTotal, module) => moduleTotal + (module.groups || []).reduce(
      (groupTotal, group) => groupTotal + (group.permissions || []).length,
      0,
    ),
    0,
  ), [permissionModules]);

  const showModal = (title, message) => setModal({ open: true, title, message });

  const handleProfileSubmit = async (event) => {
    event.preventDefault();
    setSaving(true);
    try {
      const updated = await updateAdminProfile({
        phone: form.phone || null,
        dateOfBirth: form.dateOfBirth || null,
        gender: form.gender || null,
        avatar: form.avatar || null,
      });
      setUpdatedProfile(updated);
      syncUser(updated);
      showModal("Đã cập nhật hồ sơ", "Thông tin cá nhân đã được đồng bộ với hệ thống.");
    } catch (error) {
      showModal("Không thể cập nhật", error.message);
    } finally {
      setSaving(false);
    }
  };

  const handlePasswordSubmit = async (event) => {
    event.preventDefault();
    if (passwords.newPassword !== passwords.confirmPassword) {
      showModal("Mật khẩu chưa hợp lệ", "Mật khẩu xác nhận không khớp.");
      return;
    }
    if (passwords.newPassword.length < 8) {
      showModal("Mật khẩu chưa hợp lệ", "Mật khẩu mới phải có ít nhất 8 ký tự.");
      return;
    }
    setSaving(true);
    try {
      await changeAdminPassword({ currentPassword: passwords.currentPassword, newPassword: passwords.newPassword });
      setPasswords({ currentPassword: "", newPassword: "", confirmPassword: "" });
      showModal("Đổi mật khẩu thành công", "Mật khẩu tài khoản đã được cập nhật an toàn.");
    } catch (error) {
      showModal("Không thể đổi mật khẩu", error.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="admin-catalog-page admin-profile-page">
      <AdminCatalogPageHeader
        icon={UserRound}
        eyebrow="Tài khoản nội bộ"
        title="Hồ sơ quản trị"
        description="Thông tin định danh, nhân sự và bảo mật được đồng bộ trực tiếp từ hệ thống."
        status={profile ? <><CheckCircle2 size={15} /> Đã đồng bộ dữ liệu</> : <><Clock3 size={15} /> Chưa đồng bộ</>}
      />

      {!profile ? (
        <div className="admin-profile-state"><LoaderCircle className="animate-spin" /><span>Đang tải thông tin người đăng nhập…</span></div>
      ) : (
        <>
          <section className="admin-profile-hero">
            <div className="admin-profile-avatar">
              {displayProfile.avatar
                ? <img src={displayProfile.avatar} alt={`Ảnh đại diện của ${displayProfile.fullName || displayProfile.username}`} />
                : initials(displayProfile.fullName || displayProfile.username)}
            </div>
            <div className="admin-profile-identity">
              <span>{displayProfile.employeeCode || "Tài khoản quản trị"}</span>
              <h2>{displayProfile.fullName || displayProfile.username || "Chưa xác định"}</h2>
              <p><Mail size={14} /> {displayProfile.email || "Chưa có email"}</p>
            </div>
            <div className="admin-profile-role-list">
              {(displayProfile.roles || []).length > 0
                ? displayProfile.roles.map((role) => <span key={role.code}><ShieldCheck size={13} /> {role.name || role.code}</span>)
                : <span><ShieldCheck size={13} /> Chưa được gán vai trò</span>}
            </div>
          </section>

          <nav className="admin-profile-tabs" aria-label="Nội dung hồ sơ">
            <Button variant="unstyled" className={tab === "profile" ? "is-active" : ""} onClick={() => setTab("profile")}><UserRound size={16} /> Hồ sơ cá nhân</Button>
            <Button variant="unstyled" className={tab === "security" ? "is-active" : ""} onClick={() => setTab("security")}><LockKeyhole size={16} /> Bảo mật tài khoản</Button>
            <Button variant="unstyled" className={tab === "permissions" ? "is-active" : ""} onClick={() => setTab("permissions")}><ShieldCheck size={16} /> Quyền hạn hiện có</Button>
          </nav>

          {tab === "profile" ? (
            <div className="admin-profile-layout">
              <form className="admin-profile-panel" onSubmit={handleProfileSubmit}>
                <div className="admin-profile-panel__heading">
                  <div><span>Thông tin có thể chỉnh sửa</span><h3>Thông tin cá nhân</h3></div><BadgeCheck size={20} />
                </div>
                <div className="admin-profile-form-grid">
                  <label><span>Số điện thoại</span><div><Phone size={16} /><input value={form.phone} onChange={(event) => setForm((current) => ({ ...current, phone: event.target.value }))} placeholder="Chưa cập nhật" /></div></label>
                  <label><span>Ngày sinh</span><div><CalendarDays size={16} /><input type="date" value={form.dateOfBirth} onChange={(event) => setForm((current) => ({ ...current, dateOfBirth: event.target.value }))} /></div></label>
                  <label><span>Giới tính</span><div><UserRound size={16} /><select value={form.gender} onChange={(event) => setForm((current) => ({ ...current, gender: event.target.value }))}><option value="">Chưa cập nhật</option><option value="MALE">Nam</option><option value="FEMALE">Nữ</option><option value="OTHER">Khác</option></select></div></label>
                  <label><span>URL ảnh đại diện</span><div><UserRound size={16} /><input value={form.avatar} onChange={(event) => setForm((current) => ({ ...current, avatar: event.target.value }))} placeholder="https://…" /></div></label>
                </div>
                <Button type="submit" disabled={!profile || saving} className="admin-profile-save"><Save size={16} /> {saving ? "Đang lưu…" : "Lưu thay đổi"}</Button>
              </form>

              <aside className="admin-profile-panel admin-profile-employment">
                <div className="admin-profile-panel__heading"><div><span>Dữ liệu do hệ thống cấp</span><h3>Thông tin nhân sự</h3></div><BriefcaseBusiness size={20} /></div>
                <dl>
                  <div><dt><BriefcaseBusiness size={15} /> Chức danh</dt><dd>{displayProfile.jobTitle || "—"}</dd></div>
                  <div><dt><Building2 size={15} /> Hình thức làm việc</dt><dd>{employmentTypeLabels[displayProfile.employmentType] || displayProfile.employmentType || "—"}</dd></div>
                  <div><dt><BadgeCheck size={15} /> Trạng thái</dt><dd className="is-positive">{employmentStatusLabels[displayProfile.employmentStatus] || displayProfile.employmentStatus || "—"}</dd></div>
                  <div><dt><CalendarDays size={15} /> Ngày vào làm</dt><dd>{formatDate(displayProfile.hireDate)}</dd></div>
                  <div><dt><MapPin size={15} /> Nơi làm việc</dt><dd>{displayProfile.workLocation || "—"}</dd></div>
                  <div><dt><Mail size={15} /> Email</dt><dd>{displayProfile.emailVerified ? "Đã xác minh" : "Chưa xác minh"}</dd></div>
                </dl>
              </aside>
            </div>
          ) : tab === "security" ? (
            <div className="admin-profile-security-layout">
              <form className="admin-profile-panel admin-profile-password" onSubmit={handlePasswordSubmit}>
                <div className="admin-profile-panel__heading"><div><span>Bảo vệ tài khoản</span><h3>Đổi mật khẩu</h3></div><KeyRound size={20} /></div>
                <p>Mật khẩu mới cần tối thiểu 8 ký tự và khác mật khẩu đang sử dụng.</p>
                <label><span>Mật khẩu hiện tại</span><input required type="password" autoComplete="current-password" value={passwords.currentPassword} onChange={(event) => setPasswords((current) => ({ ...current, currentPassword: event.target.value }))} /></label>
                <label><span>Mật khẩu mới</span><input required minLength={8} type="password" autoComplete="new-password" value={passwords.newPassword} onChange={(event) => setPasswords((current) => ({ ...current, newPassword: event.target.value }))} /></label>
                <label><span>Xác nhận mật khẩu mới</span><input required minLength={8} type="password" autoComplete="new-password" value={passwords.confirmPassword} onChange={(event) => setPasswords((current) => ({ ...current, confirmPassword: event.target.value }))} /></label>
                <Button type="submit" disabled={!profile || saving} className="admin-profile-save"><KeyRound size={16} /> {saving ? "Đang xử lý…" : "Cập nhật mật khẩu"}</Button>
              </form>
              <aside className="admin-profile-security-summary">
                <ShieldCheck size={26} /><span>Trạng thái bảo mật</span>
                <h3>{displayProfile.locked ? "Tài khoản đang bị khóa" : "Tài khoản được bảo vệ"}</h3>
                <dl><div><dt>Lần đăng nhập gần nhất</dt><dd>{formatDate(displayProfile.lastLogin, true)}</dd></div><div><dt>Đổi mật khẩu gần nhất</dt><dd>{formatDate(displayProfile.lastPasswordChange, true)}</dd></div><div><dt>Ngày tạo tài khoản</dt><dd>{formatDate(displayProfile.createdAt)}</dd></div></dl>
              </aside>
            </div>
          ) : (
            <div className="admin-profile-permissions">
              <section className="admin-profile-panel admin-profile-permissions__summary">
                <div>
                  <span>Phân quyền hiệu lực</span>
                  <h3>Quyền hạn của tài khoản</h3>
                  <p>Danh sách chỉ đọc, tổng hợp từ vai trò và các quyền được cấp trực tiếp cho bạn.</p>
                </div>
                <dl>
                  <div><dt>Module được truy cập</dt><dd>{permissionModules.length}</dd></div>
                  <div><dt>Tổng quyền hiện có</dt><dd>{permissionCount}</dd></div>
                </dl>
              </section>

              {permissionsLoading ? (
                <div className="admin-profile-state"><LoaderCircle className="animate-spin" /><span>Đang tải quyền hạn…</span></div>
              ) : permissionsError ? (
                <div className="admin-profile-alert" role="alert">
                  <ShieldCheck size={18} />
                  <div><strong>Không thể lấy danh sách quyền</strong><span>{permissionsError.message}</span></div>
                  <Button variant="unstyled" onClick={reloadPermissions}>Thử lại</Button>
                </div>
              ) : permissionModules.length === 0 ? (
                <div className="admin-profile-state"><ShieldCheck /><span>Tài khoản hiện chưa có quyền hạn nào.</span></div>
              ) : (
                <div className="admin-profile-permission-modules">
                  {permissionModules.map((module) => {
                    const modulePermissionCount = (module.groups || []).reduce(
                      (total, group) => total + (group.permissions || []).length,
                      0,
                    );
                    return (
                      <section key={module.code} className="admin-profile-permission-module">
                        <header>
                          <div><span>{module.code}</span><h3>{module.name}</h3></div>
                          <strong>{modulePermissionCount} quyền</strong>
                        </header>
                        <div className="admin-profile-permission-groups">
                          {(module.groups || []).map((group) => (
                            <article key={`${module.code}-${group.code}`} className="admin-profile-permission-group">
                              <div className="admin-profile-permission-group__heading">
                                <span><ShieldCheck size={15} /></span>
                                <div><h4>{group.name}</h4><code>{group.code}</code></div>
                                <small>{(group.permissions || []).length}</small>
                              </div>
                              <ul>
                                {(group.permissions || []).map((permission) => (
                                  <li key={permission.code}>
                                    <CheckCircle2 size={14} />
                                    <div><strong>{permission.name}</strong><code>{permission.code}</code></div>
                                    {permission.scope && <span>{permission.scope}</span>}
                                  </li>
                                ))}
                              </ul>
                            </article>
                          ))}
                        </div>
                      </section>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </>
      )}

      <AdminDetailDialog open={modal.open} title={modal.title} onClose={() => setModal((current) => ({ ...current, open: false }))} size="sm" showFooter>
        <p className="text-zinc-400 text-center">{modal.message}</p>
      </AdminDetailDialog>
    </div>
  );
}
