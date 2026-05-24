import { Calendar, TicketPercent, DollarSign, Truck } from "lucide-react";

export default function PromotionCard({ promotion }) {
  if (!promotion) return null;

  const {
    code,
    name,
    discount_type,
    discount_value,
    start_date,
    end_date,
    min_order_value,
    active,
  } = promotion;

  /* ===== Helpers ===== */
  const formatDate = (date) =>
    new Date(date).toLocaleDateString("vi-VN");

  const formatMoney = (value) =>
    Number(value).toLocaleString("vi-VN");

  const renderDiscount = () => {
    switch (discount_type) {
      case "percent":
        return (
          <span className="flex items-center gap-1 text-amber-400">
            <TicketPercent size={16} />
            Giảm {discount_value}%
          </span>
        );
      case "cash":
        return (
          <span className="flex items-center gap-1 text-emerald-400">
            <DollarSign size={16} />
            Giảm {formatMoney(discount_value)}đ
          </span>
        );
      case "freeship":
        return (
          <span className="flex items-center gap-1 text-sky-400">
            <Truck size={16} />
            Freeship
          </span>
        );
      default:
        return null;
    }
  };

  return (
    <div
      className="
        bg-zinc-900 border border-zinc-800 rounded-2xl p-5
        hover:border-amber-400 transition
        flex flex-col gap-4
      "
    >
      {/* ===== HEADER ===== */}
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="font-semibold text-lg line-clamp-2">
            {name}
          </h3>
          <p className="text-sm text-zinc-400 mt-1">
            Mã: <span className="text-zinc-200 font-medium">{code}</span>
          </p>
        </div>

        <span
          className={`text-xs px-3 py-1 rounded-full font-medium
            ${active
              ? "bg-emerald-400/10 text-emerald-400"
              : "bg-zinc-700 text-zinc-400"}
          `}
        >
          {active ? "Đang áp dụng" : "Ngừng hoạt động"}
        </span>
      </div>

      {/* ===== DISCOUNT ===== */}
      <div className="text-sm">
        {renderDiscount()}
      </div>

      {/* ===== CONDITIONS ===== */}
      <div className="text-sm text-zinc-400">
        Đơn tối thiểu{" "}
        <span className="text-zinc-200 font-medium">
          {formatMoney(min_order_value)}đ
        </span>
      </div>

      {/* ===== DATE ===== */}
      <div className="flex items-center gap-2 text-sm text-zinc-400">
        <Calendar size={16} />
        {formatDate(start_date)} – {formatDate(end_date)}
      </div>
    </div>
  );
}