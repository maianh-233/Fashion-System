import {
  CheckCircle2,
  Clock3,
  PackageCheck,
  XCircle,
  FileText,
} from "lucide-react";

export default function GoodsReceiptStatusHistory({
  histories = [],
}) {
  const getStatusInfo = (status) => {
    switch (status) {
      case "DRAFT":
        return {
          label: "Phiếu nháp",
          color: "text-zinc-400",
          bg: "bg-zinc-500/10",
          border: "border-zinc-500/30",
          icon: FileText,
        };

      case "PENDING":
        return {
          label: "Chờ duyệt",
          color: "text-yellow-400",
          bg: "bg-yellow-500/10",
          border: "border-yellow-500/30",
          icon: Clock3,
        };

      case "APPROVED":
        return {
          label: "Đã duyệt",
          color: "text-blue-400",
          bg: "bg-blue-500/10",
          border: "border-blue-500/30",
          icon: CheckCircle2,
        };

      case "RECEIVED":
        return {
          label: "Đã nhập kho",
          color: "text-green-400",
          bg: "bg-green-500/10",
          border: "border-green-500/30",
          icon: PackageCheck,
        };

      case "CANCELLED":
        return {
          label: "Đã hủy",
          color: "text-red-400",
          bg: "bg-red-500/10",
          border: "border-red-500/30",
          icon: XCircle,
        };

      default:
        return {
          label: status,
          color: "text-zinc-400",
          bg: "bg-zinc-500/10",
          border: "border-zinc-500/30",
          icon: Clock3,
        };
    }
  };

  return (
    <div className="rounded-2xl border border-zinc-700 bg-[#1b1b1b]">
      {/* HEADER */}

      <div className="border-b border-zinc-700 px-6 py-4">
        <h3 className="text-lg font-semibold text-orange-400">
          Lịch sử trạng thái
        </h3>

        <p className="mt-1 text-sm text-zinc-400">
          Quá trình xử lý phiếu nhập kho
        </p>
      </div>

      {/* BODY */}

      <div className="p-6">
        {histories.length === 0 ? (
          <div className="rounded-xl border border-dashed border-zinc-700 py-12 text-center">
            <Clock3
              size={44}
              className="mx-auto text-zinc-600"
            />

            <p className="mt-4 text-zinc-500">
              Chưa có lịch sử thay đổi
            </p>
          </div>
        ) : (
          <div className="relative">
            {/* Timeline */}

            <div className="absolute left-[18px] top-0 bottom-0 w-px bg-zinc-700" />

            <div className="space-y-6">
              {histories.map((history, index) => {
                const status = getStatusInfo(
                  history.status
                );

                const Icon = status.icon;

                return (
                  <div
                    key={history.id || index}
                    className="relative flex gap-5"
                  >
                    {/* DOT */}

                    <div
                      className={`relative z-10 flex h-9 w-9 items-center justify-center rounded-full border ${status.border} ${status.bg}`}
                    >
                      <Icon
                        size={18}
                        className={status.color}
                      />
                    </div>

                    {/* CONTENT */}

                    <div className="flex-1 rounded-xl border border-zinc-700 bg-zinc-900 p-5">
                      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                        <div>
                          <div
                            className={`font-semibold ${status.color}`}
                          >
                            {status.label}
                          </div>

                          <div className="mt-1 text-sm text-zinc-400">
                            {history.description ||
                              "Không có mô tả"}
                          </div>
                        </div>

                        <div className="text-right text-sm text-zinc-500">
                          <div>
                            {history.time}
                          </div>

                          <div className="mt-1">
                            {history.user}
                          </div>
                        </div>
                      </div>

                      {history.note && (
                        <div className="mt-4 rounded-lg border border-zinc-700 bg-[#181818] p-3">
                          <div className="text-xs uppercase tracking-wide text-zinc-500">
                            Ghi chú
                          </div>

                          <div className="mt-2 text-sm text-zinc-300">
                            {history.note}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}