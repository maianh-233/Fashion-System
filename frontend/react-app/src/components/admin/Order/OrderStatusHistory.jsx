import {
  Clock3,
  ArrowRight,
  CheckCircle2,
} from "lucide-react";

const STATUS_COLORS = {
  PENDING: "bg-yellow-500",
  CONFIRMED: "bg-blue-500",
  PROCESSING: "bg-indigo-500",
  SHIPPING: "bg-orange-500",
  DELIVERED: "bg-green-500",
  CANCELLED: "bg-red-500",
  RETURNED: "bg-purple-500",
};

const STATUS_LABEL = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  PROCESSING: "Đang xử lý",
  SHIPPING: "Đang giao",
  DELIVERED: "Đã giao",
  CANCELLED: "Đã hủy",
  RETURNED: "Đã hoàn trả",
};

export default function OrderStatusHistory({
  histories = [],
}) {
  const formatDate = (date) => {
    if (!date) return "--";

    return new Date(date).toLocaleString("vi-VN");
  };

  return (
    <section className="rounded-xl border border-zinc-700 bg-[#1b1b1b]">

      {/* Header */}

      <div className="flex items-center gap-3 border-b border-zinc-700 px-6 py-4">

        <Clock3
          size={22}
          className="text-orange-400"
        />

        <div>

          <h3 className="text-lg font-semibold text-orange-400">
            Lịch sử trạng thái
          </h3>

          <p className="text-sm text-zinc-400">
            Các lần thay đổi trạng thái của đơn hàng
          </p>

        </div>

      </div>

      {/* Timeline */}

      <div className="p-6">

        {histories.length === 0 ? (

          <div className="py-10 text-center text-zinc-500">
            Chưa có lịch sử thay đổi trạng thái.
          </div>

        ) : (

          <div className="relative">

            {/* Vertical line */}

            <div className="absolute left-5 top-0 bottom-0 w-[2px] bg-zinc-700" />

            <div className="space-y-8">

              {histories.map((history, index) => (

                <div
                  key={history.id || index}
                  className="relative flex gap-5"
                >

                  {/* Timeline Dot */}

                  <div
                    className={`relative z-10 flex h-10 w-10 items-center justify-center rounded-full ${
                      STATUS_COLORS[
                        history.toStatus
                      ] || "bg-zinc-500"
                    }`}
                  >
                    <CheckCircle2
                      size={18}
                      className="text-white"
                    />
                  </div>

                  {/* Content */}

                  <div className="flex-1 rounded-xl border border-zinc-700 bg-zinc-900 p-4">

                    <div className="flex flex-wrap items-center justify-between gap-3">

                      <div className="flex items-center gap-3 text-sm">

                        <span className="rounded bg-zinc-800 px-3 py-1 text-zinc-300">
                          {STATUS_LABEL[
                            history.fromStatus
                          ] ||
                            history.fromStatus}
                        </span>

                        <ArrowRight
                          size={16}
                          className="text-zinc-500"
                        />

                        <span className="rounded bg-orange-500 px-3 py-1 text-white">
                          {STATUS_LABEL[
                            history.toStatus
                          ] ||
                            history.toStatus}
                        </span>

                      </div>

                      <div className="text-sm text-zinc-500">
                        {formatDate(
                          history.changedAt
                        )}
                      </div>

                    </div>

                    <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">

                      <div>

                        <div className="text-xs text-zinc-500">
                          Người thay đổi
                        </div>

                        <div className="mt-1 text-white">
                          {history.changedByName ||
                            history.changedBy ||
                            "--"}
                        </div>

                      </div>

                      <div>

                        <div className="text-xs text-zinc-500">
                          Ghi chú
                        </div>

                        <div className="mt-1 text-zinc-300">
                          {history.note ||
                            "Không có ghi chú"}
                        </div>

                      </div>

                    </div>

                  </div>

                </div>

              ))}

            </div>

          </div>

        )}

      </div>

    </section>
  );
}