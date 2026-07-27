import { MapPin } from "lucide-react";

const PROVINCES = [
  "Hồ Chí Minh",
  "Hà Nội",
  "Đà Nẵng",
  "Cần Thơ",
];

const DISTRICTS = [
  "Quận 1",
  "Quận 3",
  "Quận Bình Thạnh",
  "Quận Gò Vấp",
];

const WARDS = [
  "Phường 1",
  "Phường 2",
  "Phường 3",
  "Phường 4",
];

export default function OrderAddressSection({
  mode = "view",
  orderType = "OFFLINE",
  address,
  onChange,
}) {
  const isView = mode === "view";

  const handleChange = (field, value) => {
    onChange?.({
      ...address,
      [field]: value,
    });
  };

  return (
    <section className="rounded-xl border border-zinc-700 bg-[#1b1b1b]">
      {/* Header */}

      <div className="flex items-center gap-3 border-b border-zinc-700 px-6 py-4">
        <MapPin
          size={22}
          className="text-orange-400"
        />

        <div>
          <h3 className="text-lg font-semibold text-orange-400">
            Địa chỉ giao hàng
          </h3>

          <p className="text-sm text-zinc-400">
            Thông tin người nhận và địa chỉ giao hàng
          </p>
        </div>
      </div>

      {/* OFFLINE NOTICE */}

      {orderType === "OFFLINE" && (
        <div className="mx-6 mt-6 rounded-lg border border-amber-500/30 bg-amber-500/10 px-4 py-3">
          <p className="text-sm text-amber-300">
            Đơn hàng OFFLINE không bắt buộc phải có địa chỉ giao hàng.
            Chỉ nhập khi khách yêu cầu giao tận nơi.
          </p>
        </div>
      )}

      {/* BODY */}

      <div className="grid grid-cols-1 gap-5 p-6 md:grid-cols-2">
        {/* Receiver */}

        <div>
          <label className="mb-2 block text-sm text-zinc-400">
            Người nhận
          </label>

          <input
            type="text"
            value={address.receiverName || ""}
            disabled={isView}
            onChange={(e) =>
              handleChange(
                "receiverName",
                e.target.value
              )
            }
            placeholder="Nhập tên người nhận"
            className={`w-full rounded-lg border px-4 py-2 ${
              isView
                ? "border-zinc-700 bg-zinc-800 text-zinc-300"
                : "border-zinc-700 bg-zinc-900 text-white focus:border-orange-500 focus:outline-none"
            }`}
          />
        </div>

        {/* Phone */}

        <div>
          <label className="mb-2 block text-sm text-zinc-400">
            Số điện thoại
          </label>

          <input
            type="text"
            value={address.receiverPhone || ""}
            disabled={isView}
            onChange={(e) =>
              handleChange(
                "receiverPhone",
                e.target.value
              )
            }
            placeholder="Nhập số điện thoại"
            className={`w-full rounded-lg border px-4 py-2 ${
              isView
                ? "border-zinc-700 bg-zinc-800 text-zinc-300"
                : "border-zinc-700 bg-zinc-900 text-white focus:border-orange-500 focus:outline-none"
            }`}
          />
        </div>

        {/* Province */}

        <div>
          <label className="mb-2 block text-sm text-zinc-400">
            Tỉnh / Thành phố
          </label>

          {isView ? (
            <input
              disabled
              value={address.province || ""}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-2 text-zinc-300"
            />
          ) : (
            <select
              value={address.province || ""}
              onChange={(e) =>
                handleChange(
                  "province",
                  e.target.value
                )
              }
              className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2 text-white focus:border-orange-500 focus:outline-none"
            >
              <option value="">
                Chọn tỉnh/thành phố
              </option>

              {PROVINCES.map((item) => (
                <option
                  key={item}
                  value={item}
                >
                  {item}
                </option>
              ))}
            </select>
          )}
        </div>

        {/* District */}

        <div>
          <label className="mb-2 block text-sm text-zinc-400">
            Quận / Huyện
          </label>

          {isView ? (
            <input
              disabled
              value={address.district || ""}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-2 text-zinc-300"
            />
          ) : (
            <select
              value={address.district || ""}
              onChange={(e) =>
                handleChange(
                  "district",
                  e.target.value
                )
              }
              className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2 text-white focus:border-orange-500 focus:outline-none"
            >
              <option value="">
                Chọn quận/huyện
              </option>

              {DISTRICTS.map((item) => (
                <option
                  key={item}
                  value={item}
                >
                  {item}
                </option>
              ))}
            </select>
          )}
        </div>

        {/* Ward */}

        <div>
          <label className="mb-2 block text-sm text-zinc-400">
            Phường / Xã
          </label>

          {isView ? (
            <input
              disabled
              value={address.ward || ""}
              className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-2 text-zinc-300"
            />
          ) : (
            <select
              value={address.ward || ""}
              onChange={(e) =>
                handleChange(
                  "ward",
                  e.target.value
                )
              }
              className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2 text-white focus:border-orange-500 focus:outline-none"
            >
              <option value="">
                Chọn phường/xã
              </option>

              {WARDS.map((item) => (
                <option
                  key={item}
                  value={item}
                >
                  {item}
                </option>
              ))}
            </select>
          )}
        </div>

        {/* Address */}

        <div className="md:col-span-2">
          <label className="mb-2 block text-sm text-zinc-400">
            Địa chỉ chi tiết
          </label>

          <textarea
            rows={4}
            disabled={isView}
            value={address.addressLine || ""}
            onChange={(e) =>
              handleChange(
                "addressLine",
                e.target.value
              )
            }
            placeholder="Số nhà, tên đường..."
            className={`w-full resize-none rounded-lg border px-4 py-3 ${
              isView
                ? "border-zinc-700 bg-zinc-800 text-zinc-300"
                : "border-zinc-700 bg-zinc-900 text-white focus:border-orange-500 focus:outline-none"
            }`}
          />
        </div>
      </div>
    </section>
  );
}