export default function RoleBasicInfor({
  role,
  mode = "view",
  onChange,
}) {
  const isView = mode === "view";

  const handleChange = (field, value) => {
    onChange?.({
      ...role,
      [field]: value,
    });
  };

  return (
    <div className="rounded-xl border border-gray-700 bg-gray-900 p-5">
      {/* HEADER */}
      <div className="mb-5">
        <h3 className="text-base font-semibold text-white">
          Thông tin role
        </h3>

        <p className="mt-1 text-sm text-gray-400">
          Thông tin cơ bản của vai trò trong hệ thống
        </p>
      </div>

      {/* FORM */}
      <div className="grid grid-cols-1 gap-5 md:grid-cols-2">

        {/* ROLE NAME */}
        <div>
          <label className="mb-2 block text-sm font-medium text-gray-300">
            Tên role
          </label>

          <input
            type="text"
            value={role?.name || ""}
            disabled={isView}
            onChange={(e) =>
              handleChange("name", e.target.value)
            }
            placeholder="VD: Administrator"
            className="
              w-full rounded-lg border border-gray-700
              bg-gray-800 px-4 py-2.5
              text-sm text-white
              outline-none
              placeholder:text-gray-500
              focus:border-orange-500
              disabled:cursor-not-allowed
              disabled:opacity-60
            "
          />
        </div>

        {/* ROLE CODE */}
        <div>
          <label className="mb-2 block text-sm font-medium text-gray-300">
            Mã role
          </label>

          <input
            type="text"
            value={role?.code || ""}
            disabled={isView}
            onChange={(e) =>
              handleChange("code", e.target.value.toUpperCase())
            }
            placeholder="VD: ADMIN"
            className="
              w-full rounded-lg border border-gray-700
              bg-gray-800 px-4 py-2.5
              text-sm text-white
              uppercase
              outline-none
              placeholder:text-gray-500
              focus:border-orange-500
              disabled:cursor-not-allowed
              disabled:opacity-60
            "
          />
        </div>

        {/* DESCRIPTION */}
        <div className="md:col-span-2">
          <label className="mb-2 block text-sm font-medium text-gray-300">
            Mô tả
          </label>

          <textarea
            rows={4}
            value={role?.description || ""}
            disabled={isView}
            onChange={(e) =>
              handleChange("description", e.target.value)
            }
            placeholder="Nhập mô tả cho role..."
            className="
              w-full resize-none rounded-lg
              border border-gray-700
              bg-gray-800 px-4 py-3
              text-sm text-white
              outline-none
              placeholder:text-gray-500
              focus:border-orange-500
              disabled:cursor-not-allowed
              disabled:opacity-60
            "
          />
        </div>
      </div>
    </div>
  );
}