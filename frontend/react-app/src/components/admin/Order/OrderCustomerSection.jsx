import { User, Search } from "lucide-react";

export default function OrderCustomerSection({
  mode = "view",
  customer,
  onChange,
}) {
  const isView = mode === "view";

  const handleChange = (field, value) => {
    onChange?.({
      ...customer,
      [field]: value,
    });
  };

  return (
    <section className="rounded-xl border border-zinc-700 bg-[#1b1b1b]">
      {/* Header */}
      <div className="flex items-center gap-3 border-b border-zinc-700 px-6 py-4">
        <User
          size={22}
          className="text-orange-400"
        />

        <div>
          <h3 className="text-lg font-semibold text-orange-400">
            Thông tin khách hàng
          </h3>

          <p className="text-sm text-zinc-400">
            Khách mua hàng của đơn hàng
          </p>
        </div>
      </div>

      <div className="p-6 space-y-6">
        {/* Guest Customer */}
        {!isView && (
          <label className="flex items-center gap-3">
            <input
              type="checkbox"
              checked={customer.guest}
              onChange={(e) =>
                handleChange("guest", e.target.checked)
              }
              className="w-4 h-4 accent-orange-500"
            />

            <span className="text-white">
              Khách lẻ (không có tài khoản)
            </span>
          </label>
        )}

        {/* Search Customer */}
        {!customer.guest && !isView && (
          <div>
            <label className="block mb-2 text-sm text-zinc-400">
              Tìm khách hàng
            </label>

            <div className="relative">
              <Search
                size={18}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500"
              />

              <input
                placeholder="Nhập tên, email hoặc số điện thoại..."
                className="w-full rounded-lg border border-zinc-700 bg-zinc-900 py-2 pl-10 pr-4 text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
              />
            </div>

            <p className="mt-2 text-xs text-zinc-500">
              (Sau này sẽ tích hợp API tìm kiếm khách hàng.)
            </p>
          </div>
        )}

        {/* Customer Info */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {/* Name */}
          <div>
            <label className="block mb-2 text-sm text-zinc-400">
              Họ và tên
            </label>

            <input
              value={customer.name || ""}
              disabled={isView}
              onChange={(e) =>
                handleChange("name", e.target.value)
              }
              placeholder="Nhập họ tên"
              className={`w-full rounded-lg border px-4 py-2 ${
                isView
                  ? "bg-zinc-800 border-zinc-700 text-zinc-300"
                  : "bg-zinc-900 border-zinc-700 text-white focus:ring-2 focus:ring-orange-500 outline-none"
              }`}
            />
          </div>

          {/* Phone */}
          <div>
            <label className="block mb-2 text-sm text-zinc-400">
              Số điện thoại
            </label>

            <input
              value={customer.phone || ""}
              disabled={isView}
              onChange={(e) =>
                handleChange("phone", e.target.value)
              }
              placeholder="Nhập số điện thoại"
              className={`w-full rounded-lg border px-4 py-2 ${
                isView
                  ? "bg-zinc-800 border-zinc-700 text-zinc-300"
                  : "bg-zinc-900 border-zinc-700 text-white focus:ring-2 focus:ring-orange-500 outline-none"
              }`}
            />
          </div>

          {/* Email */}
          <div className="md:col-span-2">
            <label className="block mb-2 text-sm text-zinc-400">
              Email
            </label>

            <input
              type="email"
              value={customer.email || ""}
              disabled={isView}
              onChange={(e) =>
                handleChange("email", e.target.value)
              }
              placeholder="Nhập email"
              className={`w-full rounded-lg border px-4 py-2 ${
                isView
                  ? "bg-zinc-800 border-zinc-700 text-zinc-300"
                  : "bg-zinc-900 border-zinc-700 text-white focus:ring-2 focus:ring-orange-500 outline-none"
              }`}
            />
          </div>
        </div>

        {/* Summary */}
        {isView && (
          <div className="rounded-lg bg-zinc-800 border border-zinc-700 p-4">
            <h4 className="text-sm font-semibold text-orange-400 mb-3">
              Thông tin nhanh
            </h4>

            <div className="grid grid-cols-2 gap-3 text-sm">
              <div className="text-zinc-400">
                Loại khách:
              </div>

              <div className="text-white">
                {customer.guest ? "Khách lẻ" : "Thành viên"}
              </div>

              <div className="text-zinc-400">
                Tên:
              </div>

              <div className="text-white">
                {customer.name || "--"}
              </div>

              <div className="text-zinc-400">
                Số điện thoại:
              </div>

              <div className="text-white">
                {customer.phone || "--"}
              </div>

              <div className="text-zinc-400">
                Email:
              </div>

              <div className="text-white break-all">
                {customer.email || "--"}
              </div>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}