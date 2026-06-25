import UserInfoCard from "./UserInfoCard";

const ROLE_OPTIONS = [
  "Quản lý",
  "Nhân viên bán hàng",
  "Nhân viên kho",
  "Kế toán",
];

export default function GeneralInfoSection({ employee, mode }) {
  const isView = mode === "view";

  return (
    <section>
      <h3 className="text-lg font-semibold text-white mb-5">
        Thông tin chung
      </h3>

      <div className="grid grid-cols-2 gap-6">

        {/* 👁 VIEW MODE → CARD NGƯỜI DÙNG */}
        {isView ? (
          <UserInfoCard employee={employee} />
        ) : (
          <>
            <Input
              label="Email"
              value={employee?.email || ""}
              placeholder="email@company.com"
            />

            <Input
              label="Số điện thoại"
              value={employee?.phone || ""}
              placeholder="0123 456 789"
            />
          </>
        )}

        {/* ROLE SELECT */}
        <Select
          label="Vai trò"
          value={employee?.role || ""}
          options={ROLE_OPTIONS}
          disabled={isView}
        />
      </div>
    </section>
  );
}

function Select({ label, value, options, disabled }) {
  return (
    <div>
      <label className="block text-sm text-zinc-400 mb-2">
        {label}
      </label>

      <select
        value={value}
        disabled={disabled}
        className={`w-full px-5 py-4 rounded-2xl text-white border border-zinc-700
          ${disabled
            ? "bg-zinc-800 cursor-not-allowed"
            : "bg-zinc-800 focus:border-orange-500 focus:outline-none"}
        `}
      >
        <option value="" disabled>
          -- Chọn vai trò --
        </option>

        {options.map((opt) => (
          <option key={opt} value={opt}>
            {opt}
          </option>
        ))}
      </select>
    </div>
  );
}

function Input({ label, readOnly, ...props }) {
  return (
    <div>
      <label className="block text-sm text-zinc-400 mb-2">{label}</label>
      <input
        {...props}
        readOnly={readOnly}
        className={`w-full px-5 py-4 rounded-2xl text-white
          ${readOnly ? "bg-zinc-800" : "bg-zinc-800 focus:border-orange-500"}
          border border-zinc-700`}
      />
    </div>
  );
}

