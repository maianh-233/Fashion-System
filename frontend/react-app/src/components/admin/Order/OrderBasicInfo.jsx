import { FileText } from "lucide-react";

const ORDER_TYPES = ["OFFLINE", "ONLINE", "PICKUP"];

const ORDER_STATUS = [
  "PENDING",
  "CONFIRMED",
  "PROCESSING",
  "SHIPPING",
  "DELIVERED",
  "CANCELLED",
  "RETURNED",
];

const PAYMENT_STATUS = [
  "UNPAID",
  "PAID",
  "FAILED",
  "REFUNDED",
];

export default function OrderBasicInfo({
  mode = "view",
  order,
  onChange,
}) {
  const isView = mode === "view";

  const handleChange = (field, value) => {
    onChange?.(field, value);
  };

  const formatDate = (date) => {
    if (!date) return "--";

    return new Date(date).toLocaleString("vi-VN");
  };

  return (
    <section className="rounded-xl border border-zinc-700 bg-[#1b1b1b]">
      {/* Header */}
      <div className="flex items-center gap-3 border-b border-zinc-700 px-6 py-4">
        <FileText className="text-orange-400" size={22} />

        <div>
          <h3 className="text-lg font-semibold text-orange-400">
            Thông tin đơn hàng
          </h3>

          <p className="text-sm text-zinc-400">
            Thông tin chung của đơn hàng
          </p>
        </div>
      </div>

      {/* Body */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5 p-6">

        {/* Order Code */}
        <div>
          <label className="block mb-2 text-sm text-zinc-400">
            Mã đơn hàng
          </label>

          <input
            value={order.orderCode || ""}
            disabled
            className="w-full rounded-lg bg-zinc-800 border border-zinc-700 px-4 py-2 text-white"
          />
        </div>

        {/* Order Type */}
        <div>
          <label className="block mb-2 text-sm text-zinc-400">
            Loại đơn
          </label>

          {isView ? (
            <input
              value={order.orderType}
              disabled
              className="w-full rounded-lg bg-zinc-800 border border-zinc-700 px-4 py-2 text-white"
            />
          ) : (
            <select
              value={order.orderType}
              onChange={(e) =>
                handleChange("orderType", e.target.value)
              }
              className="w-full rounded-lg bg-zinc-900 border border-zinc-700 px-4 py-2 text-white"
            >
              {ORDER_TYPES.map((type) => (
                <option
                  key={type}
                  value={type}
                >
                  {type}
                </option>
              ))}
            </select>
          )}
        </div>

        {/* Order Status */}
        <div>
          <label className="block mb-2 text-sm text-zinc-400">
            Trạng thái
          </label>

          {isView ? (
            <input
              value={order.status}
              disabled
              className="w-full rounded-lg bg-zinc-800 border border-zinc-700 px-4 py-2 text-white"
            />
          ) : (
            <select
              value={order.status}
              onChange={(e) =>
                handleChange("status", e.target.value)
              }
              className="w-full rounded-lg bg-zinc-900 border border-zinc-700 px-4 py-2 text-white"
            >
              {ORDER_STATUS.map((status) => (
                <option
                  key={status}
                  value={status}
                >
                  {status}
                </option>
              ))}
            </select>
          )}
        </div>

        {/* Payment Status */}
        <div>
          <label className="block mb-2 text-sm text-zinc-400">
            Trạng thái thanh toán
          </label>

          {isView ? (
            <input
              value={order.paymentStatus}
              disabled
              className="w-full rounded-lg bg-zinc-800 border border-zinc-700 px-4 py-2 text-white"
            />
          ) : (
            <select
              value={order.paymentStatus}
              onChange={(e) =>
                handleChange("paymentStatus", e.target.value)
              }
              className="w-full rounded-lg bg-zinc-900 border border-zinc-700 px-4 py-2 text-white"
            >
              {PAYMENT_STATUS.map((status) => (
                <option
                  key={status}
                  value={status}
                >
                  {status}
                </option>
              ))}
            </select>
          )}
        </div>

        {/* Created At */}
        <div>
          <label className="block mb-2 text-sm text-zinc-400">
            Ngày tạo
          </label>

          <input
            value={formatDate(order.createdAt)}
            disabled
            className="w-full rounded-lg bg-zinc-800 border border-zinc-700 px-4 py-2 text-white"
          />
        </div>

        {/* Updated At */}
        <div>
          <label className="block mb-2 text-sm text-zinc-400">
            Cập nhật lần cuối
          </label>

          <input
            value={formatDate(order.updatedAt)}
            disabled
            className="w-full rounded-lg bg-zinc-800 border border-zinc-700 px-4 py-2 text-white"
          />
        </div>

      </div>

      {/* Note */}
      <div className="px-6 pb-6">
        <label className="block mb-2 text-sm text-zinc-400">
          Ghi chú
        </label>

        <textarea
          rows={4}
          disabled={isView}
          value={order.note || ""}
          onChange={(e) =>
            handleChange("note", e.target.value)
          }
          placeholder="Nhập ghi chú..."
          className={`w-full rounded-lg border px-4 py-3 resize-none ${
            isView
              ? "bg-zinc-800 border-zinc-700 text-zinc-300"
              : "bg-zinc-900 border-zinc-700 text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
          }`}
        />
      </div>
    </section>
  );
}