import { useState } from "react";
import ChatModal from "../../components/customer/Chat/ChatModal";
import OrderFilter from "../../components/customer/Order/OrderFilter";
import OrderList from "../../components/customer/Order/OrderList";
import { orders as mockOrders } from "../../hooks/orders.mock";

export default function MyOrdersPage() {
  const [status, setStatus] = useState("");
  const [chatOrderId, setChatOrderId] = useState(null);

  const filteredOrders = status
    ? mockOrders.filter((o) => o.status === status)
    : mockOrders;

  return (
    <div className="min-h-screen w-full bg-zinc-950 text-zinc-200">
      <div className="w-full px-3 sm:px-6 lg:px-10 py-4 sm:py-6">

        {/* HEADER */}
        <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-6 sm:mb-8">
          <h1 className="text-2xl sm:text-3xl font-bold">
            Đơn Hàng Của Tôi
          </h1>

          <div className="w-full sm:w-auto">
            <OrderFilter value={status} onChange={setStatus} />
          </div>
        </div>

        {/* LIST */}
        <div className="h-[calc(100vh-180px)] overflow-y-auto [-ms-overflow-style:none] [scrollbar-width:none]">
          <OrderList
            orders={filteredOrders}
            onChat={setChatOrderId}
            onCancel={(id) => alert(`Hủy đơn ${id}`)}
          />
        </div>

        {/* CHAT MODAL */}
        {chatOrderId && (
          <ChatModal
            orderId={chatOrderId}
            onClose={() => setChatOrderId(null)}
          />
        )}
      </div>
    </div>
  );
}