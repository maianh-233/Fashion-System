import { CreditCard } from "lucide-react";

const PAYMENT_STATUS_OPTIONS = [
  "UNPAID",
  "PAID",
  "FAILED",
  "REFUNDED",
];

const PAYMENT_METHOD_OPTIONS = [
  { value: "CASH", label: "Tiền mặt" },
  { value: "BANK_TRANSFER", label: "Chuyển khoản" },
  { value: "VNPAY", label: "VNPay" },
  { value: "MOMO", label: "MoMo" },
];

export default function OrderPaymentSection({
  mode = "view",
  paymentStatus,
  paymentMethod,
  transactionCode,
  paidAt,
  onStatusChange,
  onMethodChange,
  onTransactionChange,
}) {
  const isView = mode === "view";

  const formatDate = (date) => {
    if (!date) return "--";
    return new Date(date).toLocaleString("vi-VN");
  };

  const getStatusColor = (status) => {
    switch (status) {
      case "PAID":
        return "text-green-400";
      case "UNPAID":
        return "text-yellow-400";
      case "FAILED":
        return "text-red-400";
      case "REFUNDED":
        return "text-blue-400";
      default:
        return "text-zinc-300";
    }
  };

  return (
    <section className="rounded-xl border border-zinc-700 bg-[#1b1b1b]">
      {/* Header */}
      <div className="flex items-center gap-3 border-b border-zinc-700 px-6 py-4">
        <CreditCard size={22} className="text-orange-400" />

        <div>
          <h3 className="text-lg font-semibold text-orange-400">
            Thanh toán
          </h3>

          <p className="text-sm text-zinc-400">
            Thông tin thanh toán của đơn hàng
          </p>
        </div>
      </div>

      {/* Body */}
      <div className="grid grid-cols-1 gap-5 p-6 md:grid-cols-2">
        {/* Payment Status */}
        <div>
          <label className="mb-2 block text-sm text-zinc-400">
            Trạng thái thanh toán
          </label>

          {isView ? (
            <div
              className={`rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-2 font-medium ${getStatusColor(
                paymentStatus
              )}`}
            >
              {paymentStatus || "--"}
            </div>
          ) : (
            <select
              value={paymentStatus || "UNPAID"}
              onChange={(e) => onStatusChange?.(e.target.value)}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2 text-white focus:border-orange-500 focus:outline-none"
            >
              {PAYMENT_STATUS_OPTIONS.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          )}
        </div>

        {/* Payment Method */}
        <div>
          <label className="mb-2 block text-sm text-zinc-400">
            Phương thức thanh toán
          </label>

          {isView ? (
            <input
              disabled
              value={paymentMethod || "--"}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-2 text-zinc-300"
            />
          ) : (
            <select
              value={paymentMethod || "CASH"}
              onChange={(e) => onMethodChange?.(e.target.value)}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2 text-white focus:border-orange-500 focus:outline-none"
            >
              {PAYMENT_METHOD_OPTIONS.map((method) => (
                <option key={method.value} value={method.value}>
                  {method.label}
                </option>
              ))}
            </select>
          )}
        </div>

        {/* Transaction Code */}
        <div className="md:col-span-2">
          <label className="mb-2 block text-sm text-zinc-400">
            Mã giao dịch
          </label>

          <input
            type="text"
            disabled={isView}
            value={transactionCode || ""}
            onChange={(e) =>
              onTransactionChange?.(e.target.value)
            }
            placeholder="Nhập mã giao dịch / mã tham chiếu"
            className={`w-full rounded-lg border px-4 py-2 ${
              isView
                ? "border-zinc-700 bg-zinc-800 text-zinc-300"
                : "border-zinc-700 bg-zinc-900 text-white focus:border-orange-500 focus:outline-none"
            }`}
          />
        </div>

        {/* Paid At */}
        <div className="md:col-span-2">
          <label className="mb-2 block text-sm text-zinc-400">
            Thời gian thanh toán
          </label>

          <input
            disabled
            value={formatDate(paidAt)}
            className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-2 text-zinc-300"
          />
        </div>
      </div>

      {/* Summary */}
      <div className="border-t border-zinc-700 px-6 py-4">
        <div className="flex items-center justify-between rounded-lg bg-zinc-800 px-4 py-3">
          <span className="text-zinc-400">
            Tình trạng hiện tại
          </span>

          <span
            className={`font-semibold ${getStatusColor(
              paymentStatus
            )}`}
          >
            {paymentStatus === "PAID"
              ? "Đã thanh toán"
              : paymentStatus === "UNPAID"
              ? "Chưa thanh toán"
              : paymentStatus === "FAILED"
              ? "Thanh toán thất bại"
              : paymentStatus === "REFUNDED"
              ? "Đã hoàn tiền"
              : "Không xác định"}
          </span>
        </div>
      </div>
    </section>
  );
}