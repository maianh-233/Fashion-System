export default function OrderFilter({ value, onChange }) {
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      aria-label="Lọc đơn hàng theo trạng thái"
      className="min-h-11 w-full rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-sm focus:border-amber-400 focus:outline-none sm:w-auto sm:rounded-2xl sm:px-5 sm:py-3"
    >
      <option value="">Tất cả đơn hàng</option>
      <option value="PENDING">Chờ xác nhận</option>
      <option value="CONFIRMED">Đã xác nhận</option>
      <option value="SHIPPING">Đang giao hàng</option>
      <option value="DELIVERED">Đã nhận hàng</option>
      <option value="CANCELLED">Đã hủy</option>
    </select>
  );
}
