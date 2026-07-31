import { CalendarDays, FileText, Package, User } from "lucide-react";

export default function GoodsReceiptBasicInfo({
  mode = "view",
  receipt,
  onChange,
}) {
  const isView = mode === "view";

  const handleChange = (field, value) => {
    onChange?.(field, value);
  };

  return (
    <div className="rounded-2xl border border-zinc-700 bg-[#1b1b1b]">
      {/* HEADER */}
      <div className="border-b border-zinc-700 px-6 py-4">
        <h3 className="text-lg font-semibold text-orange-400">
          Thông tin phiếu nhập
        </h3>

        <p className="mt-1 text-sm text-zinc-400">
          Thông tin cơ bản của phiếu nhập kho
        </p>
      </div>

      {/* BODY */}

      <div className="grid grid-cols-1 gap-6 p-6 md:grid-cols-2">
        {/* Receipt Code */}

        <div>
          <label className="mb-2 flex items-center gap-2 text-sm text-zinc-400">
            <FileText size={16} />
            Mã phiếu nhập
          </label>

          <input
            type="text"
            value={receipt.receiptCode || ""}
            disabled={isView}
            onChange={(e) =>
              handleChange("receiptCode", e.target.value)
            }
            placeholder="PN000001"
            className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2 text-white outline-none focus:border-orange-500"
          />
        </div>

        {/* Receipt Date */}

        <div>
          <label className="mb-2 flex items-center gap-2 text-sm text-zinc-400">
            <CalendarDays size={16} />
            Ngày nhập
          </label>

          <input
            type="date"
            value={receipt.receiptDate || ""}
            disabled={isView}
            onChange={(e) =>
              handleChange("receiptDate", e.target.value)
            }
            className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2 text-white outline-none focus:border-orange-500"
          />
        </div>

        {/* Status */}

        <div>
          <label className="mb-2 flex items-center gap-2 text-sm text-zinc-400">
            <Package size={16} />
            Trạng thái
          </label>

          <select
            value={receipt.status || "DRAFT"}
            disabled={isView}
            onChange={(e) =>
              handleChange("status", e.target.value)
            }
            className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2 text-white outline-none focus:border-orange-500"
          >
            <option value="DRAFT">Nháp</option>
            <option value="PENDING">Chờ duyệt</option>
            <option value="APPROVED">Đã duyệt</option>
            <option value="RECEIVED">Đã nhập kho</option>
            <option value="CANCELLED">Đã hủy</option>
          </select>
        </div>

        {/* Received By */}

        <div>
          <label className="mb-2 flex items-center gap-2 text-sm text-zinc-400">
            <User size={16} />
            Người nhận
          </label>

          <input
            type="text"
            value={receipt.receivedBy || ""}
            disabled={isView}
            onChange={(e) =>
              handleChange("receivedBy", e.target.value)
            }
            placeholder="Nhân viên kho"
            className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2 text-white outline-none focus:border-orange-500"
          />
        </div>

        {/* Approved By */}

        <div>
          <label className="mb-2 flex items-center gap-2 text-sm text-zinc-400">
            <User size={16} />
            Người duyệt
          </label>

          <input
            type="text"
            value={receipt.approvedBy || ""}
            disabled={isView}
            onChange={(e) =>
              handleChange("approvedBy", e.target.value)
            }
            placeholder="Quản lý"
            className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2 text-white outline-none focus:border-orange-500"
          />
        </div>

        {/* Total Quantity */}

        <div>
          <label className="mb-2 text-sm text-zinc-400">
            Tổng số lượng
          </label>

          <input
            type="number"
            disabled
            value={receipt.totalQuantity || 0}
            className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-2 text-orange-400 font-semibold"
          />
        </div>

        {/* Total Amount */}

        <div>
          <label className="mb-2 text-sm text-zinc-400">
            Tổng tiền nhập
          </label>

          <input
            type="text"
            disabled
            value={Number(
              receipt.totalAmount || 0
            ).toLocaleString("vi-VN")}
            className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-2 font-semibold text-orange-400"
          />
        </div>

        {/* Note */}

        <div className="md:col-span-2">
          <label className="mb-2 block text-sm text-zinc-400">
            Ghi chú
          </label>

          <textarea
            rows={4}
            value={receipt.note || ""}
            disabled={isView}
            onChange={(e) =>
              handleChange("note", e.target.value)
            }
            placeholder="Nhập ghi chú..."
            className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-3 text-white outline-none focus:border-orange-500 resize-none"
          />
        </div>
      </div>
    </div>
  );
}