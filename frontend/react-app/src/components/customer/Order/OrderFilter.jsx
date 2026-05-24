export default function OrderFilter({ value, onChange }) {
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className="bg-zinc-900 border border-zinc-700 rounded-2xl px-5 py-3 text-sm"
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