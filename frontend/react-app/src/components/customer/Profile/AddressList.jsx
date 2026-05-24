import {
  MapPin,
  Plus,
  Trash2,
  Star,
  Phone,
  User,
  Home,
  Building2,
  Map
} from "lucide-react";

/* helper map icon theo loại địa chỉ */
const AddressTypeIcon = ({ type }) => {
  switch (type) {
    case "HOME":
      return <Home size={14} />;
    case "WORK":
      return <Building2 size={14} />;
    default:
      return <Map size={14} />;
  }
};

export default function AddressList({
  addresses,
  onAdd,
  onDelete,
  onSetDefault,
}) {
  return (
    <div>
      {/* HEADER */}
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-semibold flex items-center gap-3">
          <MapPin className="text-amber-400" />
          Địa chỉ của tôi
        </h2>

        <button
          onClick={onAdd}
          className="bg-amber-500 hover:bg-amber-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium text-black"
        >
          <Plus size={20} />
          Thêm địa chỉ
        </button>
      </div>

      {/* HORIZONTAL SCROLL – NO SCROLLBAR */}
      <div
        style={{
          overflowX: "auto",
          scrollbarWidth: "none",
          msOverflowStyle: "none",
        }}
      >
        <div className="flex gap-4 pb-2">
          {addresses.map((addr) => {
            const isDefault = addr.is_default;

            return (
              <div
                key={addr.id}
                className={`
                  min-w-[360px] p-6 rounded-3xl flex-shrink-0
                  bg-zinc-800 border
                  ${isDefault
                    ? "border-amber-400 ring-2 ring-amber-400/40"
                    : "border-zinc-700"}
                `}
              >
                {/* TOP */}
                <div className="flex justify-between items-start">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 font-semibold text-zinc-100">
                      <User size={16} className="text-amber-400" />
                      {addr.receiver_name}
                    </div>

                    <div className="flex items-center gap-2 text-sm text-zinc-400">
                      <Phone size={14} />
                      {addr.receiver_phone}
                    </div>
                  </div>

                  {isDefault && (
                    <span className="flex items-center gap-1 bg-amber-400 text-black text-xs px-3 py-1 rounded-full">
                      <Star size={12} />
                      Mặc định
                    </span>
                  )}
                </div>

                {/* TYPE */}
                <div className="mt-4">
                  <span className="inline-flex items-center gap-2 text-xs px-3 py-1 rounded-full bg-zinc-700 text-zinc-300">
                    <AddressTypeIcon type={addr.address_type} />
                    {addr.address_type}
                  </span>
                </div>

                {/* ADDRESS */}
                <div className="mt-3 text-sm text-zinc-300 leading-relaxed">
                  <p>{addr.address_line}</p>
                  <p className="text-zinc-400 mt-1">
                    {addr.ward}, {addr.district}, {addr.province}
                  </p>
                </div>

                {addr.postal_code && (
                  <p className="text-xs text-zinc-500 mt-1">
                    Mã bưu điện: {addr.postal_code}
                  </p>
                )}

                {/* ACTIONS */}
                <div className="flex justify-between items-center mt-5">
                  {!isDefault && (
                    <button
                      onClick={() => onSetDefault(addr.id)}
                      className="text-amber-400 text-sm hover:text-amber-300"
                    >
                      Đặt làm mặc định
                    </button>
                  )}

                  <Trash2
                    onClick={() => onDelete(addr.id)}
                    className="text-red-400 cursor-pointer hover:text-red-500"
                  />
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}