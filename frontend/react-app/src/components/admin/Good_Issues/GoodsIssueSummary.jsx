import { Boxes } from "lucide-react";

export default function GoodsIssueSummary({
  summary,
}) {
  return (
    <div className="rounded-2xl border border-zinc-700 bg-[#1b1b1b] p-6">
      {/* ================= HEADER ================= */}

      <div className="mb-6 flex items-center gap-3">
        <Boxes
          size={22}
          className="text-orange-400"
        />

        <div>
          <h3 className="text-lg font-semibold text-white">
            Tổng kết phiếu xuất
          </h3>

          <p className="text-sm text-zinc-500">
            Thống kê số lượng hàng đã xuất
          </p>
        </div>
      </div>

      {/* ================= SUMMARY ================= */}

      <div className="grid grid-cols-1 gap-5 md:grid-cols-3">
        {/* Tổng số lượng */}

        <div className="rounded-xl border border-zinc-700 bg-zinc-900 p-5">
          <p className="text-sm text-zinc-400">
            Tổng số lượng xuất
          </p>

          <p className="mt-2 text-3xl font-bold text-orange-400">
            {summary.totalQuantity || 0}
          </p>
        </div>

        {/* Số dòng sản phẩm */}

        <div className="rounded-xl border border-zinc-700 bg-zinc-900 p-5">
          <p className="text-sm text-zinc-400">
            Số dòng sản phẩm
          </p>

          <p className="mt-2 text-3xl font-bold text-white">
            {summary.totalItems || 0}
          </p>
        </div>

        {/* Trạng thái */}

        <div className="rounded-xl border border-zinc-700 bg-zinc-900 p-5">
          <p className="text-sm text-zinc-400">
            Trạng thái
          </p>

          <p className="mt-2 text-lg font-semibold text-green-400">
            Sẵn sàng xuất kho
          </p>
        </div>
      </div>
    </div>
  );
}