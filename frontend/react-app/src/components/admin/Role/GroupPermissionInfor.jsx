import { ShieldCheck } from "lucide-react";

const SCOPES = [
  ["SELF", "Cá nhân"],
  ["TEAM", "Nhóm trực thuộc"],
  ["DEPARTMENT", "Phòng ban"],
  ["STORE", "Cửa hàng"],
  ["ALL", "Toàn hệ thống"],
];

export default function GroupPermissionInfor({
  groupPermission,
  permissions = [],
  selectedPermissions = {},
  mode = "view",
  onPermissionChange,
}) {
  const readOnly = mode === "view";

  const togglePermission = (permissionId) => {
    if (readOnly) return;
    const next = { ...selectedPermissions };
    if (next[permissionId]) delete next[permissionId];
    else next[permissionId] = "ALL";
    onPermissionChange?.(next);
  };

  const changeScope = (permissionId, scope) => {
    if (!readOnly) onPermissionChange?.({ ...selectedPermissions, [permissionId]: scope });
  };

  return (
    <section className="rounded-xl border border-gray-700 bg-gray-900">
      <header className="flex items-start gap-3 border-b border-gray-700 p-5">
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-orange-500/10 text-orange-400">
          <ShieldCheck size={20} />
        </span>
        <div className="min-w-0">
          <h3 className="font-semibold text-white">{groupPermission?.name}</h3>
          <div className="mt-1 flex flex-wrap gap-2">
            <code className="rounded-md bg-gray-800 px-2 py-1 text-xs font-medium text-orange-400">{groupPermission?.code}</code>
            {groupPermission?.moduleName && <span className="rounded-md bg-gray-800 px-2 py-1 text-xs text-gray-400">{groupPermission.moduleName}</span>}
          </div>
        </div>
      </header>

      <div className="space-y-2 p-5">
        {permissions.length === 0 ? (
          <div className="rounded-lg border border-dashed border-gray-700 px-4 py-6 text-center text-sm text-gray-500">Nhóm này chưa có permission.</div>
        ) : permissions.map((permission) => {
          const scope = selectedPermissions[permission.id];
          return (
            <div key={permission.id} className={`flex flex-col gap-3 rounded-lg border p-4 sm:flex-row sm:items-center ${scope ? "border-orange-500/40 bg-orange-500/5" : "border-gray-800 bg-gray-800/40"}`}>
              <label className={`flex min-w-0 flex-1 items-start gap-3 ${readOnly ? "cursor-default" : "cursor-pointer"}`}>
                <input type="checkbox" checked={Boolean(scope)} disabled={readOnly} onChange={() => togglePermission(permission.id)} className="mt-1 h-4 w-4 accent-orange-500" />
                <span className="min-w-0">
                  <span className="block text-sm font-medium text-gray-200">{permission.name}</span>
                  <code className="text-xs text-gray-500">{permission.code}</code>
                </span>
              </label>
              {scope && (
                <select value={scope} disabled={readOnly} onChange={(event) => changeScope(permission.id, event.target.value)} className="rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-xs text-white disabled:opacity-70">
                  {SCOPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select>
              )}
            </div>
          );
        })}
      </div>
    </section>
  );
}
