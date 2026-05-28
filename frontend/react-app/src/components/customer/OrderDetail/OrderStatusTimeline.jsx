import {
  Package,
  CheckCircle2,
  Truck,
  BadgeCheck,
  XCircle,
} from "lucide-react";

const STATUS_META = {
  CREATED: {
    label: "Đã tạo đơn",
    icon: Package,
    color: "text-zinc-400",
  },
  CONFIRMED: {
    label: "Đã xác nhận",
    icon: CheckCircle2,
    color: "text-blue-400",
  },
  SHIPPING: {
    label: "Đang giao",
    icon: Truck,
    color: "text-yellow-400",
  },
  DELIVERED: {
    label: "Hoàn tất",
    icon: BadgeCheck,
    color: "text-green-400",
  },
  CANCELLED: {
    label: "Đã hủy",
    icon: XCircle,
    color: "text-red-400",
  },
};
export default function OrderStatusTimeline({ status, history }) {
  return (
    <div className="bg-zinc-900 rounded-2xl p-6">
      <h2 className="text-xl font-semibold mb-6">
        Trạng thái đơn hàng
      </h2>

      <ol className="relative border-l border-zinc-700 ml-3 space-y-6">
        {history.map((item, idx) => {
          const meta = STATUS_META[item.status];
          const Icon = meta.icon;
          const isActive = item.status === status;

          return (
            <li key={idx} className="ml-6">
              {/* ICON */}
              <span
                className={`
                  absolute -left-3 flex items-center justify-center
                  w-6 h-6 rounded-full bg-zinc-800 border
                  ${isActive
                    ? "border-green-400 shadow-[0_0_12px_#22c55e]"
                    : "border-zinc-600"}
                `}
              >
                <Icon
                  size={14}
                  className={
                    isActive ? "text-green-400" : meta.color
                  }
                />
              </span>

              {/* CONTENT */}
              <div
                className={`
                  rounded-xl px-4 py-3
                  ${isActive
                    ? "bg-zinc-800/80"
                    : "bg-zinc-800/40"}
                `}
              >
                <p className="font-medium flex items-center gap-2">
                  <span
                    className={
                      isActive ? "text-green-400" : "text-white"
                    }
                  >
                    {meta.label}
                  </span>
                </p>

                <p className="text-sm text-zinc-400 mt-1">
                  {new Date(item.time).toLocaleString("vi-VN", {
                    hour: "2-digit",
                    minute: "2-digit",
                    day: "2-digit",
                    month: "2-digit",
                    year: "numeric",
                  })}
                </p>
              </div>
            </li>
          );
        })}
      </ol>
    </div>
  );
}