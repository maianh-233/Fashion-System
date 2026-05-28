import { Home } from "lucide-react";
export default function SavedAddresses({ addresses, onSelect }) {
  return (
    <div className="bg-zinc-900 rounded-2xl p-6">
      <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
        <Home size={26} />
        <span>Địa chỉ đã lưu</span>
      </h2>

      {/* WRAPPER SCROLL NGANG */}
      <div
        className="overflow-x-auto"
        style={{
          scrollbarWidth: "none",   // Firefox
          msOverflowStyle: "none",  // IE
        }}
      >
        {/* Chrome / Edge / Safari */}
        <style>
          {`
            div::-webkit-scrollbar {
              display: none;
            }
          `}
        </style>

        {/* LIST NGANG */}
        <div className="grid grid-flow-col auto-cols-[260px] gap-4">
          {addresses.map(addr => (
            <div
              key={addr.id}
              onClick={() => onSelect(addr)}
              className={`border rounded-2xl p-4 cursor-pointer
                ${
                  addr.is_default
                    ? "border-orange-500 bg-orange-950/30"
                    : "border-zinc-700"
                }`}
            >
              <p className="font-medium">{addr.receiver_name}</p>
              <p className="text-sm text-gray-400">{addr.receiver_phone}</p>
              <p className="text-sm mt-2 text-gray-300">
                {addr.address_line}, {addr.ward}, {addr.district}
              </p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}