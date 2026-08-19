export default function RolePermissionAudit({ entries = [], permissions = [] }) {
  const permissionMap = new Map(permissions.map((permission) => [permission.id, permission]));

  return (
    <section className="rbac-section">
      <div className="rbac-section__heading">
        <div>
          <h3>Lịch sử thay đổi quyền</h3>
          <p>Dữ liệu chỉ đọc từ role_permission_audit.</p>
        </div>
      </div>
      {entries.length === 0 ? (
        <div className="rbac-empty">Chưa có thay đổi quyền nào.</div>
      ) : (
        <div className="overflow-x-auto">
          <table className="rbac-audit-table">
            <thead><tr><th>Hành động</th><th>Quyền</th><th>Người thực hiện</th><th>Thời điểm</th></tr></thead>
            <tbody>
              {entries.map((entry) => {
                const permission = permissionMap.get(entry.permission_id);
                return (
                  <tr key={entry.id}>
                    <td><span className={`rbac-audit-action rbac-audit-action--${entry.action?.toLowerCase()}`}>{entry.action}</span></td>
                    <td><strong>{permission?.name || entry.permission_id}</strong><small>{permission?.code}</small></td>
                    <td>{entry.changed_by}</td><td>{entry.changed_at}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
