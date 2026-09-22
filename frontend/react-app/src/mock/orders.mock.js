const STATUSES = [
  "PENDING",
  "CONFIRMED",
  "SHIPPING",
  "DELIVERED",
  "CANCELLED",
];

export const orders = Array.from({ length: 28 }, (_, index) => {
  const orderNumber = String(index + 1).padStart(3, "0");
  const subtotal = 720000 + (index % 7) * 285000;
  const discountTotal = index % 3 === 0 ? 120000 : 0;
  const shippingFee = index % 4 === 0 ? 0 : 35000;
  const status = STATUSES[index % STATUSES.length];

  return {
    id: `order-${orderNumber}`,
    order_code: `ORD-20260817-${orderNumber}`,
    created_at: new Date(2026, 7, 17 - index, 9 + (index % 8), 15).toISOString(),
    status,
    payment_status:
      status === "DELIVERED" || index % 3 === 1 ? "PAID" : "UNPAID",
    order_type: index % 4 === 0 ? "PICKUP" : "ONLINE",
    subtotal,
    discount_total: discountTotal,
    shipping_fee: shippingFee,
    total_amount: subtotal - discountTotal + shippingFee,
    item_count: 1 + (index % 5),
    note: index % 5 === 0 ? "Vui lòng gọi trước khi giao hàng." : "",
    thumbnail: `https://picsum.photos/seed/lunaria-order-${index + 1}/300/300`,
  };
});
