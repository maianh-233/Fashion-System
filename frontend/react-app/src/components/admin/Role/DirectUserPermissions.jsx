export default function DirectUserPermissions({
  users = [],
  assignedUserIds = [],
  permissionGroups = [],
  directPermissions = {},
  selectedUserId,
  onSelectedUserChange,
  onChange,
  readOnly,
}) {
  const assignedUsers = users.filter((user) => assignedUserIds.includes(user.id));
  const selectedIds = directPermissions[selectedUserId] || [];

  const togglePermission = (permissionId) => {
    if (readOnly || !selectedUserId) return;
    const next = selectedIds.includes(permissionId)
      ? selectedIds.filter((id) => id !== permissionId)
      : [...selectedIds, permissionId];
    onChange?.({ ...directPermissions, [selectedUserId]: next });
  };

  return (
    <section className="rbac-section">
      <div className="rbac-section__heading">
        <div>
          <h3>Quyền cấp trực tiếp</h3>
          <p>Ánh xạ bảng user_permissions; các quyền này được cộng thêm ngoài quyền từ role.</p>
        </div>
      </div>

      <label className="admin-field mb-5">
        <span className="admin-field__label">Người dùng</span>
        <select
          className="admin-field__control"
          value={selectedUserId || ""}
          onChange={(event) => onSelectedUserChange?.(event.target.value)}
        >
          <option value="">Chọn người dùng đã được gán role</option>
          {assignedUsers.map((user) => <option key={user.id} value={user.id}>{user.email}</option>)}
        </select>
      </label>

      {!selectedUserId ? (
        <div className="rbac-empty">Chọn người dùng để cấu hình quyền trực tiếp.</div>
      ) : (
        <div className="space-y-4">
          {permissionGroups.map((group) => (
            <div key={group.id} className="rbac-permission-group">
              <div className="rbac-permission-group__title">
                <strong>{group.name}</strong><code>{group.code}</code>
              </div>
              <div className="rbac-permission-grid">
                {(group.permissions || []).map((permission) => (
                  <label key={permission.id} className="rbac-permission">
                    <input
                      type="checkbox"
                      checked={selectedIds.includes(permission.id)}
                      disabled={readOnly}
                      onChange={() => togglePermission(permission.id)}
                    />
                    <span><strong>{permission.name}</strong><small>{permission.code}</small></span>
                  </label>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
