import { useState, useMemo } from "react";
import { LoaderCircle, PackageSearch } from "lucide-react";
import ChatModal from "../../components/customer/Chat/ChatModal";
import OrderFilter from "../../components/customer/Order/OrderFilter";
import OrderList from "../../components/customer/Order/OrderList";
import Pagination from "../../components/common/Pagination";
import { orders as mockOrders } from "../../hooks/orders.mock";
import { useMobileInfiniteList } from "../../hooks/useMobileInfiniteList";

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
  const safeTotalPages = Math.max(1, totalPages);
  const safePage = Math.min(page, safeTotalPages);
  const {
    visibleCount,
    isLoadingMore,
    hasMore,
    loadMoreRef,
    reset,
  } = useMobileInfiniteList(filteredOrders.length, {
    initialCount: PAGE_SIZE,
    batchSize: 4,
  });

  const pagedOrders = useMemo(() => {
    return filteredOrders.slice(
      (safePage - 1) * PAGE_SIZE,
      safePage * PAGE_SIZE
    );
  }, [filteredOrders, safePage]);

  const mobileOrders = filteredOrders.slice(0, visibleCount);

  return (
    <div className="min-h-screen w-full bg-zinc-950 text-zinc-200">
      <div className="mx-auto w-full max-w-[1440px] px-4 py-5 sm:px-6 sm:py-8 lg:px-10">

        {/* HEADER */}
        <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-6 sm:mb-8">
          <div>
            <h1 className="text-2xl font-bold sm:text-3xl">Đơn hàng của tôi</h1>
            <p className="mt-1 text-sm text-zinc-400">
              {filteredOrders.length} đơn hàng
            </p>
          </div>

          <div className="w-full sm:w-auto">
            <OrderFilter
              value={status}
              onChange={(v) => {
                setStatus(v);
                setPage(1);
                reset();
              }}
            />
          </div>
        </div>

        {filteredOrders.length === 0 ? (
          <div className="flex min-h-72 flex-col items-center justify-center text-center text-zinc-500">
            <PackageSearch className="mb-3" size={36} />
            <p>Không có đơn hàng nào ở trạng thái này.</p>
          </div>
        ) : (
          <>
            <div className="sm:hidden">
              <OrderList
                orders={mobileOrders}
                onChat={setChatOrderId}
                onCancel={(id) => alert(`Hủy đơn ${id}`)}
              />
            </div>

            <div className="hidden sm:block">
              <OrderList
                orders={pagedOrders}
                onChat={setChatOrderId}
                onCancel={(id) => alert(`Hủy đơn ${id}`)}
              />
            </div>

            <div
              ref={loadMoreRef}
              className="flex min-h-20 items-center justify-center sm:hidden"
              aria-live="polite"
            >
              {isLoadingMore && (
                <span className="flex items-center gap-2 text-sm text-zinc-400">
                  <LoaderCircle className="animate-spin" size={18} />
                  Đang tải thêm đơn hàng...
                </span>
              )}
              {!hasMore && (
                <span className="text-sm text-zinc-500">
                  Bạn đã xem hết {filteredOrders.length} đơn hàng
                </span>
              )}
            </div>
          </>
        )}

        {/* PAGINATION (DESKTOP ONLY) */}
        {filteredOrders.length > 0 && (
          <div className="mt-8 hidden sm:block">
            <Pagination
              currentPage={safePage}
              totalPages={safeTotalPages}
              onPageChange={setPage}
            />
          </div>
        )}

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
