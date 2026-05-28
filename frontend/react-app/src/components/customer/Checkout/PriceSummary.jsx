export default function PriceSummary({ order }) {
  if (!order) return null;

  const total =
    order.subtotal -
    order.discount_total +
    order.shipping_fee +
    order.tax;

  return (
    <div className="space-y-3 text-sm">
      <Row label="Tạm tính" value={order.subtotal} />
      <Row label="Giảm giá" value={-order.discount_total} green />
      <Row label="Phí ship" value={order.shipping_fee} />
      <Row label="Thuế" value={order.tax} />

      <hr className="border-zinc-700" />

      <div className="flex justify-between text-lg font-bold">
        <span>Tổng</span>
        <span>{total.toLocaleString()} ₫</span>
      </div>
    </div>
  );
}

const Row = ({ label, value, green }) => (
  <div className={`flex justify-between ${green && "text-green-400"}`}>
    <span>{label}</span>
    <span>{value.toLocaleString()} ₫</span>
  </div>
);