import { useState } from "react";
import { Truck, Store, TypeIcon } from "lucide-react";

export default function OrderTypeSelector({
  value,
  onChange,
  onSelectStore,
  stores = [],
}) {
  const [keyword, setKeyword] = useState("");

  const handleSearch = e => {
    if (e.key === "Enter") {
      const found = stores.find(s =>
        s.name.toLowerCase().includes(keyword.toLowerCase())
      );
      if (found) onSelectStore(found);
    }
  };

  return (
    <div className="bg-zinc-900 rounded-2xl p-6 space-y-4">
      <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
        <TypeIcon size={26} />
        <span>Chọn loại đơn hàng</span>
      </h2>

      {/* CHỌN LOẠI */}
      <div className="grid grid-cols-2 gap-4">
        <button
          onClick={() => onChange("ONLINE")}
          className={`flex items-center gap-2 justify-center py-3 rounded-xl border
            ${value === "ONLINE"
              ? "bg-blue-600 border-blue-500"
              : "border-zinc-700 hover:border-zinc-500"}`}
        >
          <Truck size={18} />
          Giao hàng
        </button>

        <button
          onClick={() => onChange("PICKUP")}
          className={`flex items-center gap-2 justify-center py-3 rounded-xl border
            ${value === "PICKUP"
              ? "bg-green-600 border-green-500"
              : "border-zinc-700 hover:border-zinc-500"}`}
        >
          <Store size={18} />
          Nhận tại cửa hàng
        </button>
      </div>

      {/* PICKUP INPUT */}
      {value === "PICKUP" && (
        <div className="space-y-2">
          <label className="text-sm text-gray-400">
            Nhập tên cửa hàng (Enter để chọn)
          </label>
          <input
            value={keyword}
            onChange={e => setKeyword(e.target.value)}
            onKeyDown={handleSearch}
            placeholder="VD: Store Quận 1"
            className="w-full px-4 py-3 rounded-xl bg-zinc-800 border border-zinc-700 focus:outline-none focus:border-green-500"
          />
        </div>
      )}
    </div>
  );
}