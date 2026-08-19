import { KeyRound, ScrollText, ShieldCheck, UserRound } from "lucide-react";

export default function RoleOverview({ role, users = [], userIds = [], permissionIds = [], directPermissions = {}, auditEntries = [] }) {
  const assignedUsers = users.filter((user) => userIds.includes(user.id));
  const directCount = Object.values(directPermissions).reduce((total, ids) => total + ids.length, 0);
  const stats = [
    { label: "Người dùng", value: userIds.length, icon: UserRound },
    { label: "Quyền theo role", value: permissionIds.length, icon: ShieldCheck },
    { label: "Quyền trực tiếp", value: directCount, icon: KeyRound },
    { label: "Lần thay đổi", value: auditEntries.length, icon: ScrollText },
  ];

  return (
    <div className="space-y-5">
      <section className="rbac-overview-hero">
        <div>
          <span className="rbac-overview-code">{role.code || "CHƯA_CÓ_MÃ"}</span>
          <h3>{role.name || "Vai trò chưa đặt tên"}</h3>
          <p>{role.description || "Chưa có mô tả cho vai trò này."}</p>
        </div>
        <dl>
          <div><dt>ID</dt><dd>{role.id || "Sẽ tạo tự động"}</dd></div>
          <div><dt>Ngày tạo</dt><dd>{role.created_at || "Chưa khởi tạo"}</dd></div>
        </dl>
      </section>

      <div className="rbac-overview-stats">
        {stats.map(({ label, value, icon: Icon }) => (
          <div key={label} className="rbac-overview-stat"><Icon size={20} /><span><strong>{value}</strong><small>{label}</small></span></div>
        ))}
      </div>

      <section className="rbac-section">
        <div className="rbac-section__heading"><div><h3>Người dùng đang được gán</h3><p>Tóm tắt quan hệ trong bảng user_roles.</p></div></div>
        {assignedUsers.length ? (
          <div className="rbac-overview-users">
            {assignedUsers.map((user) => <span key={user.id}><strong>{user.name}</strong><small>{user.email}</small></span>)}
          </div>
        ) : <div className="rbac-empty">Vai trò này chưa được gán cho người dùng nào.</div>}
      </section>
    </div>
  );
}
