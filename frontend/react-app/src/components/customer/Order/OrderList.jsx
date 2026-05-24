import OrderCard from "./OrderCard";

export default function OrderList({ orders, onChat, onCancel }) {
  return (
    <div className="space-y-6">
      {orders.map((order) => (
        <OrderCard
          key={order.id}
          order={order}
          onChat={onChat}
          onCancel={onCancel}
        />
      ))}
    </div>
  );
}
