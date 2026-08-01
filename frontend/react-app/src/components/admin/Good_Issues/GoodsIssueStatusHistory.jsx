import {
  Clock,
  CheckCircle2,
  XCircle,
  Package,
  Truck,
} from "lucide-react";

export default function GoodsIssueStatusHistory({
  histories = [],
}) {
  const getStatusConfig = (status) => {
    switch (status) {
      case "PENDING":
        return {
          label: "Chờ duyệt",
          color: "text-yellow-400",
          bg: "bg-yellow-500/10",
          icon: (
            <Clock
              size={18}
              className="text-yellow-400"
            />
          ),
        };

      case "APPROVED":
        return {
          label: "Đã duyệt",
          color: "text-blue-400",
          bg: "bg-blue-500/10",
          icon: (
            <CheckCircle2
              size={18}
              className="text-blue-400"
            />
          ),
        };

      case "ISSUED":
        return {
          label: "Đã xuất kho",
          color: "text-green-400",
          bg: "bg-green-500/10",
          icon: (
            <Truck
              size={18}
              className="text-green-400"
            />
          ),
        };

      case "CANCELLED":
        return {
          label: "Đã hủy",
          color: "text-red-400",
          bg: "bg-red-500/10",
          icon: (
            <XCircle
              size={18}
              className="text-red-400"
            />
          ),
        };

      default:
        return {
          label: status,
          color: "text-zinc-400",
          bg: "bg-zinc-700",
          icon: (
            <Package
              size={18}
              className="text-zinc-400"
            />
          ),
        };
    }
  };

  return (
    <div className="rounded-2xl border border-zinc-700 bg-[#1b1b1b] p-6">
      {/* ================= HEADER ================= */}

      <div className="mb-6 flex items-center gap-3">
        <Clock
          size={22}
          className="text-orange-400"
        />

        <div>
          <h3 className="text-lg font-semibold text-white">
            Lịch sử trạng thái
          </h3>

          <p className="text-sm text-zinc-500">
            Theo dõi quá trình xử lý phiếu xuất
          </p>
        </div>
      </div>

      {/* ================= EMPTY ================= */}

      {histories.length === 0 ? (
        <div className="rounded-xl border border-dashed border-zinc-700 py-10 text-center text-zinc-500">
          Chưa có lịch sử trạng thái
        </div>
      ) : (
        <div className="space-y-4">
          {histories.map((history, index) => {
            const status = getStatusConfig(
              history.status
            );

            return (
              <div
                key={history.id ?? index}
                className="flex gap-4"
              >
                {/* Timeline */}

                <div className="flex flex-col items-center">
                  <div
                    className={`flex h-10 w-10 items-center justify-center rounded-full ${status.bg}`}
                  >
                    {status.icon}
                  </div>

                  {index !==
                    histories.length - 1 && (
                    <div className="mt-2 h-full w-px bg-zinc-700" />
                  )}
                </div>

                {/* Content */}

                <div className="flex-1 rounded-xl border border-zinc-700 bg-zinc-900 p-4">
                  <div className="flex items-center justify-between">
                    <span
                      className={`font-semibold ${status.color}`}
                    >
                      {status.label}
                    </span>

                    <span className="text-sm text-zinc-500">
                      {history.time}
                    </span>
                  </div>

                  <div className="mt-2 text-sm text-zinc-300">
                    {history.description}
                  </div>

                  {history.user && (
                    <div className="mt-3 text-xs text-zinc-500">
                      Thực hiện bởi:{" "}
                      <span className="text-zinc-300">
                        {history.user}
                      </span>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}