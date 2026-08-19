import { ShieldCheck } from "lucide-react";

export default function GroupPermissionInfor({
  groupPermission,
  permissions = [],
  selectedPermissionIds = [],
  mode = "view",
  onPermissionChange,
}) {
  const isView = mode === "view";

  const isChecked = (permissionId) => {
    return selectedPermissionIds.includes(permissionId);
  };

  const handleCheckboxChange = (permissionId) => {
    if (isView) return;

    const checked = isChecked(permissionId);

    let newSelectedIds;

    if (checked) {
      newSelectedIds = selectedPermissionIds.filter(
        (id) => id !== permissionId
      );
    } else {
      newSelectedIds = [
        ...selectedPermissionIds,
        permissionId,
      ];
    }

    onPermissionChange?.(newSelectedIds);
  };

  return (
    <div className="rounded-xl border border-gray-700 bg-gray-900">

      {/* GROUP INFORMATION */}
      <div className="border-b border-gray-700 p-5">

        <div className="flex items-start gap-3">

          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-orange-500/10 text-orange-400">
            <ShieldCheck size={20} />
          </div>

          <div className="min-w-0">
            <h3 className="font-semibold text-white">
              {groupPermission?.name}
            </h3>

            <div className="mt-1">
              <span className="rounded-md bg-gray-800 px-2 py-1 text-xs font-medium text-orange-400">
                {groupPermission?.code}
              </span>
            </div>

            {groupPermission?.description && (
              <p className="mt-2 text-sm text-gray-400">
                {groupPermission.description}
              </p>
            )}
          </div>

        </div>

      </div>

      {/* PERMISSION LIST */}
      <div className="p-5">

        <div className="mb-3">
          <h4 className="text-sm font-semibold text-gray-200">
            Danh sách quyền
          </h4>

          <p className="mt-1 text-xs text-gray-500">
            Chọn các quyền mà role này được phép sử dụng
          </p>
        </div>

        <div className="space-y-2">

          {permissions.length === 0 ? (
            <div className="rounded-lg border border-dashed border-gray-700 px-4 py-6 text-center text-sm text-gray-500">
              Nhóm quyền này chưa có permission.
            </div>
          ) : (
            permissions.map((permission) => {
              const checked = isChecked(permission.id);

              return (
                <label
                  key={permission.id}
                  className={`
                    flex cursor-pointer items-start gap-3
                    rounded-lg border p-4
                    transition
                    ${
                      checked
                        ? "border-orange-500/40 bg-orange-500/5"
                        : "border-gray-800 bg-gray-800/40 hover:border-gray-700"
                    }
                    ${
                      isView
                        ? "cursor-default"
                        : ""
                    }
                  `}
                >

                  {/* CHECKBOX */}
                  <input
                    type="checkbox"
                    checked={checked}
                    disabled={isView}
                    onChange={() =>
                      handleCheckboxChange(permission.id)
                    }
                    className="
                      mt-1 h-4 w-4
                      rounded border-gray-600
                      bg-gray-800
                      text-orange-500
                      focus:ring-orange-500
                      disabled:cursor-not-allowed
                    "
                  />

                  {/* PERMISSION INFO */}
                  <div className="min-w-0 flex-1">

                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-sm font-medium text-gray-200">
                        {permission.name}
                      </span>

                      <span className="rounded bg-gray-700 px-2 py-0.5 text-[11px] font-medium text-gray-400">
                        {permission.code}
                      </span>
                    </div>

                    {permission.description && (
                      <p className="mt-1 text-xs text-gray-500">
                        {permission.description}
                      </p>
                    )}

                  </div>

                </label>
              );
            })
          )}

        </div>
      </div>
    </div>
  );
}