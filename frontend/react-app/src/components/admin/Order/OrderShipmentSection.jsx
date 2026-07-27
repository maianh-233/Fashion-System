import { Truck } from "lucide-react";

const SHIPPING_STATUS = [
  "PENDING",
  "SHIPPING",
  "DELIVERED",
  "FAILED",
  "RETURNED",
];

export default function OrderShipmentSection({
  mode = "view",
  shipment = {},
  onChange,
}) {
  const isView = mode === "view";

  const handleChange = (field, value) => {
    onChange?.({
      ...shipment,
      [field]: value,
    });
  };

  const formatDate = (date) => {
    if (!date) return "--";

    return new Date(date).toLocaleString("vi-VN");
  };

  return (
    <section className="rounded-xl border border-zinc-700 bg-[#1b1b1b]">
      {/* Header */}

      <div className="flex items-center gap-3 border-b border-zinc-700 px-6 py-4">
        <Truck
          size={22}
          className="text-orange-400"
        />

        <div>
          <h3 className="text-lg font-semibold text-orange-400">
            Thông tin vận chuyển
          </h3>

          <p className="text-sm text-zinc-400">
            Thông tin giao hàng của đơn hàng
          </p>
        </div>
      </div>

      {/* Body */}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-5 p-6">

        {/* Shipping Provider */}

        <div>
          <label className="block mb-2 text-sm text-zinc-400">
            Đơn vị vận chuyển
          </label>

          <input
            value={shipment.shippingProvider || ""}
            disabled={isView}
            onChange={(e) =>
              handleChange(
                "shippingProvider",
                e.target.value
              )
            }
            placeholder="VD: Giao Hàng Nhanh"
            className={`w-full rounded-lg border px-4 py-2 ${
              isView
                ? "bg-zinc-800 border-zinc-700 text-zinc-300"
                : "bg-zinc-900 border-zinc-700 text-white focus:outline-none focus:border-orange-500"
            }`}
          />
        </div>

        {/* Tracking */}

        <div>
          <label className="block mb-2 text-sm text-zinc-400">
            Mã vận đơn
          </label>

          <input
            value={shipment.trackingCode || ""}
            disabled={isView}
            onChange={(e) =>
              handleChange(
                "trackingCode",
                e.target.value
              )
            }
            placeholder="Tracking Code"
            className={`w-full rounded-lg border px-4 py-2 ${
              isView
                ? "bg-zinc-800 border-zinc-700 text-zinc-300"
                : "bg-zinc-900 border-zinc-700 text-white focus:outline-none focus:border-orange-500"
            }`}
          />
        </div>

        {/* Shipping Status */}

        <div>
          <label className="block mb-2 text-sm text-zinc-400">
            Trạng thái giao hàng
          </label>

          {isView ? (
            <input
              value={shipment.shippingStatus || ""}
              disabled
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-2 text-zinc-300"
            />
          ) : (
            <select
              value={shipment.shippingStatus || ""}
              onChange={(e) =>
                handleChange(
                  "shippingStatus",
                  e.target.value
                )
              }
              className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2 text-white focus:outline-none focus:border-orange-500"
            >
              {SHIPPING_STATUS.map((status) => (
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

        {/* Created */}

        <div>
          <label className="block mb-2 text-sm text-zinc-400">
            Ngày tạo
          </label>

          <input
            disabled
            value={formatDate(shipment.createdAt)}
            className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-2 text-zinc-300"
          />
        </div>

        {/* Shipped */}

        <div>
          <label className="block mb-2 text-sm text-zinc-400">
            Ngày bàn giao
          </label>

          {isView ? (
            <input
              disabled
              value={formatDate(shipment.shippedAt)}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-2 text-zinc-300"
            />
          ) : (
            <input
              type="datetime-local"
              value={shipment.shippedAt || ""}
              onChange={(e) =>
                handleChange(
                  "shippedAt",
                  e.target.value
                )
              }
              className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2 text-white focus:outline-none focus:border-orange-500"
            />
          )}
        </div>

        {/* Delivered */}

        <div>
          <label className="block mb-2 text-sm text-zinc-400">
            Ngày giao thành công
          </label>

          {isView ? (
            <input
              disabled
              value={formatDate(
                shipment.deliveredAt
              )}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-2 text-zinc-300"
            />
          ) : (
            <input
              type="datetime-local"
              value={shipment.deliveredAt || ""}
              onChange={(e) =>
                handleChange(
                  "deliveredAt",
                  e.target.value
                )
              }
              className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2 text-white focus:outline-none focus:border-orange-500"
            />
          )}
        </div>

      </div>
    </section>
  );
}