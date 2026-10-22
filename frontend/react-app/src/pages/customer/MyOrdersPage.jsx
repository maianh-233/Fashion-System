import { useState } from "react";
import { Navigate } from "react-router-dom";
import { PackageSearch } from "lucide-react";
import ChatModal from "../../components/customer/Chat/ChatModal";
import OrderCard from "../../components/customer/Order/OrderCard";
import OrderFilter from "../../components/customer/Order/OrderFilter";
import CancelOrderDialog from "../../components/customer/Order/CancelOrderDialog";
import ServerPagedList from "../../components/customer/ServerPagedList";
import { useSystemNotification } from "../../components/common/SystemNotification";
import { customerOrderApi } from "../../api/customerApi";
import { useCustomerSession } from "../../hooks/useCustomerSession";
import { useServerPages } from "../../hooks/useServerPages";

const PAGE_SIZE = 6;

/** Đơn hàng của khách (API): lọc trạng thái, thanh toán lại, hủy, chat hỗ trợ. */
export default function MyOrdersPage() {
  const { loggedIn } = useCustomerSession();
  const notification = useSystemNotification();
  const [status, setStatus] = useState("");
  const [version, setVersion] = useState(0);
  const [chatOrder, setChatOrder] = useState(null);
  const [cancelOrder, setCancelOrder] = useState(null);
  const [paying, setPaying] = useState(null);

  const list = useServerPages(`${status}#${version}`, (page, options) =>
    customerOrderApi.list({ status, page: page - 1, size: PAGE_SIZE }, options));

  if (!loggedIn) return <Navigate to={`/customerlogin?redirect=${encodeURIComponent("/orders")}`} replace />;

  const pay = async (order) => {
    setPaying(order.id);
    try {
      const payment = await customerOrderApi.pay(order.id);
      window.location.assign(payment.payUrl);
    } catch (failure) {
      notification.error(failure.message);
      setPaying(null);
    }
  };

  return (
    <div className="customer-page min-h-screen w-full text-zinc-200">
      <div className="customer-page__wide px-4 py-5 sm:px-6 sm:py-8">
        <div className="mb-6 flex flex-col gap-4 sm:mb-8 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="customer-page-title">Đơn hàng của tôi</h1>
            <p className="mt-1 text-sm text-zinc-400">{list.loaded ? `${list.totalElements} đơn hàng` : "Đang tải..."}</p>
          </div>
          <div className="w-full sm:w-auto">
            <OrderFilter value={status} onChange={setStatus} />
          </div>
        </div>

        <ServerPagedList
          list={list}
          itemLabel="đơn hàng"
          emptyIcon={PackageSearch}
          emptyText={status ? "Không có đơn hàng nào ở trạng thái này." : "Bạn chưa có đơn hàng nào."}
          desktopClassName="hidden space-y-6 sm:block"
          mobileClassName="space-y-4 sm:hidden"
          skeleton={<div className="customer-card customer-card--skeleton" />}
          renderItem={(order) => (
            <OrderCard key={order.id} order={order} busy={paying === order.id ? "pay" : null}
              onChat={setChatOrder} onCancel={setCancelOrder} onPay={pay} />
          )}
          onPageChange={() => window.scrollTo({ top: 0, behavior: "smooth" })}
        />

        {chatOrder && <ChatModal order={chatOrder} onClose={() => setChatOrder(null)} />}
        {cancelOrder && (
          <CancelOrderDialog order={cancelOrder} onClose={() => setCancelOrder(null)}
            onCancelled={(updated) => {
              setCancelOrder(null);
              notification.success(`Đã hủy đơn #${updated.orderCode}.`);
              setVersion((value) => value + 1);
            }} />
        )}
      </div>
    </div>
  );
}
