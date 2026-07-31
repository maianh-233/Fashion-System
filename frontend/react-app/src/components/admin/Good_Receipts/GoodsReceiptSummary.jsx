import {
  Boxes,
  DollarSign,
  Package,
  Receipt,
} from "lucide-react";

export default function GoodsReceiptSummary({
  summary = {},
}) {
  const {
    totalQuantity = 0,
    totalAmount = 0,
  } = summary;

  const averagePrice =
    totalQuantity > 0
      ? totalAmount / totalQuantity
      : 0;

  return (
    <div className="rounded-2xl border border-zinc-700 bg-[#1b1b1b]">
      {/* HEADER */}

      <div className="border-b border-zinc-700 px-6 py-4">
        <h3 className="text-lg font-semibold text-orange-400">
          Tổng kết phiếu nhập
        </h3>

        <p className="mt-1 text-sm text-zinc-400">
          Thống kê nhanh giá trị phiếu nhập kho
        </p>
      </div>

      {/* BODY */}

      <div className="grid grid-cols-1 gap-5 p-6 md:grid-cols-2 xl:grid-cols-4">
        {/* PRODUCT */}

        <div className="rounded-xl border border-zinc-700 bg-zinc-900 p-5">
          <div className="flex items-center justify-between">
            <Package
              className="text-orange-400"
              size={24}
            />

            <span className="text-xs text-zinc-500">
              Products
            </span>
          </div>

          <div className="mt-5 text-3xl font-bold text-white">
            {totalQuantity}
          </div>

          <div className="mt-2 text-sm text-zinc-500">
            Tổng số lượng nhập
          </div>
        </div>

        {/* TOTAL */}

        <div className="rounded-xl border border-zinc-700 bg-zinc-900 p-5">
          <div className="flex items-center justify-between">
            <DollarSign
              className="text-green-400"
              size={24}
            />

            <span className="text-xs text-zinc-500">
              Amount
            </span>
          </div>

          <div className="mt-5 text-2xl font-bold text-green-400">
            {Number(totalAmount).toLocaleString(
              "vi-VN"
            )}
            ₫
          </div>

          <div className="mt-2 text-sm text-zinc-500">
            Tổng tiền nhập
          </div>
        </div>

        {/* AVG */}

        <div className="rounded-xl border border-zinc-700 bg-zinc-900 p-5">
          <div className="flex items-center justify-between">
            <Receipt
              className="text-sky-400"
              size={24}
            />

            <span className="text-xs text-zinc-500">
              Average
            </span>
          </div>

          <div className="mt-5 text-2xl font-bold text-sky-400">
            {Number(averagePrice).toLocaleString(
              "vi-VN"
            )}
            ₫
          </div>

          <div className="mt-2 text-sm text-zinc-500">
            Giá nhập trung bình
          </div>
        </div>

        {/* VALUE */}

        <div className="rounded-xl border border-zinc-700 bg-zinc-900 p-5">
          <div className="flex items-center justify-between">
            <Boxes
              className="text-purple-400"
              size={24}
            />

            <span className="text-xs text-zinc-500">
              Receipt
            </span>
          </div>

          <div className="mt-5 text-2xl font-bold text-purple-400">
            {totalQuantity > 0
              ? "Đã có dữ liệu"
              : "Trống"}
          </div>

          <div className="mt-2 text-sm text-zinc-500">
            Trạng thái phiếu
          </div>
        </div>
      </div>

      {/* FOOTER */}

      <div className="border-t border-zinc-700 bg-[#181818] px-6 py-5">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <div className="rounded-lg border border-zinc-700 bg-zinc-900 p-4">
            <div className="text-sm text-zinc-400">
              Tổng số lượng
            </div>

            <div className="mt-2 text-xl font-bold text-orange-400">
              {totalQuantity}
            </div>
          </div>

          <div className="rounded-lg border border-zinc-700 bg-zinc-900 p-4">
            <div className="text-sm text-zinc-400">
              Tổng tiền nhập
            </div>

            <div className="mt-2 text-xl font-bold text-green-400">
              {Number(totalAmount).toLocaleString(
                "vi-VN"
              )}
              ₫
            </div>
          </div>

          <div className="rounded-lg border border-zinc-700 bg-zinc-900 p-4">
            <div className="text-sm text-zinc-400">
              Giá nhập TB
            </div>

            <div className="mt-2 text-xl font-bold text-sky-400">
              {Number(averagePrice).toLocaleString(
                "vi-VN"
              )}
              ₫
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}