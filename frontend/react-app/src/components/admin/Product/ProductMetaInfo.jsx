import { Calendar, Clock } from "lucide-react";

export default function ProductMetaInfo({ createdAt, updatedAt }) {
  const formatDateTime = (value) => {
    if (!value) return "-";
    return new Date(value).toLocaleString("vi-VN");
  };

  return (
    <section>
      <h3 className="text-lg font-semibold mb-4 text-orange-400">
        Thông tin hệ thống
      </h3>

      <div className="grid grid-cols-2 gap-4">
        {/* CREATED AT */}
        <MetaItem
          label="Ngày tạo"
          value={formatDateTime(createdAt)}
          icon={<Calendar size={18} />}
        />

        {/* UPDATED AT */}
        <MetaItem
          label="Ngày cập nhật"
          value={formatDateTime(updatedAt)}
          icon={<Clock size={18} />}
        />
      </div>
    </section>
  );
}

function MetaItem({ label, value, icon }) {
  return (
    <div className="flex items-center gap-3 p-4 bg-[#1e1e1e] border border-gray-700 rounded-lg">
      <div className="text-orange-400">{icon}</div>
      <div>
        <p className="text-sm text-gray-400">{label}</p>
        <p className="text-sm">{value}</p>
      </div>
    </div>
  );
}