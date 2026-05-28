import { useState, useMemo } from "react";
import ChatModal from "../../components/customer/Chat/ChatModal";
import OrderFilter from "../../components/customer/Order/OrderFilter";
import OrderList from "../../components/customer/Order/OrderList";
import Pagination from "../../components/common/Pagination";
import { orders as mockOrders } from "../../hooks/orders.mock";

const PAGE_SIZE = 6;

export default function MyOrdersPage() {
  const [status, setStatus] = useState("");
  const [chatOrderId, setChatOrderId] = useState(null);
  const [page, setPage] = useState(1);

  const filteredOrders = useMemo(() => {
    return status
      ? mockOrders.filter((o) => o.status === status)
      : mockOrders;
  }, [status]);

  const totalPages = Math.ceil(filteredOrders.length / PAGE_SIZE);

  const pagedOrders = useMemo(() => {
    return filteredOrders.slice(
      (page - 1) * PAGE_SIZE,
      page * PAGE_SIZE
    );
  }, [filteredOrders, page]);

  return (
    <div className="min-h-screen w-full bg-zinc-950 text-zinc-200">
      <div className="w-full px-3 sm:px-6 lg:px-10 py-4 sm:py-6">

        {/* HEADER */}
        <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-6 sm:mb-8">
          <h1 className="text-2xl sm:text-3xl font-bold">
            Đơn Hàng Của Tôi
          </h1>

          <div className="w-full sm:w-auto">
            <OrderFilter
              value={status}
              onChange={(v) => {
                setStatus(v);
                setPage(1); // reset page khi đổi filter
              }}
            />
          </div>
        </div>

        {/* LIST */}
        <div className="h-[calc(100vh-230px)] overflow-y-auto">
          <OrderList
            orders={pagedOrders}
            onChat={setChatOrderId}
            onCancel={(id) => alert(`Hủy đơn ${id}`)}
          />
        </div>

        {/* PAGINATION (DESKTOP ONLY) */}
        <Pagination
          currentPage={page}
          totalPages={totalPages}
          onPageChange={setPage}
        />

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