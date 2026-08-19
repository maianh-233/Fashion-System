import { Search, UserRound, LockKeyhole, CheckCircle2 } from "lucide-react";
import { useMemo, useState } from "react";

export default function RoleUserAssignments({ users = [], selectedUserIds = [], readOnly, onChange }) {
  const [keyword, setKeyword] = useState("");
  const filteredUsers = useMemo(() => {
    const term = keyword.trim().toLowerCase();
    if (!term) return users;
    return users.filter((user) =>
      [user.email, user.phone, user.name].some((value) => value?.toLowerCase().includes(term)),
    );
  }, [keyword, users]);

  const toggleUser = (userId) => {
    if (readOnly) return;
    onChange?.(
      selectedUserIds.includes(userId)
        ? selectedUserIds.filter((id) => id !== userId)
        : [...selectedUserIds, userId],
    );
  };

  return (
    <section className="rbac-section">
      <div className="rbac-section__heading">
        <div>
          <h3>Người dùng thuộc vai trò</h3>
          <p>Quản lý quan hệ user_roles và thời điểm gán vai trò.</p>
        </div>
        <span className="rbac-count">{selectedUserIds.length} đã chọn</span>
      </div>

      <div className="relative mb-4">
        <Search size={17} className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
        <input
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="Tìm theo email, điện thoại hoặc tên..."
          className="admin-field__control pl-10"
        />
      </div>

      <div className="rbac-user-list">
        {filteredUsers.map((user) => {
          const selected = selectedUserIds.includes(user.id);
          return (
            <label key={user.id} className={`rbac-user ${selected ? "rbac-user--selected" : ""}`}>
              <input type="checkbox" checked={selected} disabled={readOnly} onChange={() => toggleUser(user.id)} />
              <span className="rbac-user__avatar"><UserRound size={17} /></span>
              <span className="min-w-0 flex-1">
                <strong>{user.name || user.email}</strong>
                <small>{user.email} {user.phone ? `• ${user.phone}` : ""}</small>
              </span>
              <span className={`rbac-status ${user.locked ? "rbac-status--locked" : "rbac-status--active"}`}>
                {user.locked ? <LockKeyhole size={13} /> : <CheckCircle2 size={13} />}
                {user.locked ? "Đã khóa" : user.active ? "Hoạt động" : "Tạm dừng"}
              </span>
            </label>
          );
        })}
      </div>
    </section>
  );
}
