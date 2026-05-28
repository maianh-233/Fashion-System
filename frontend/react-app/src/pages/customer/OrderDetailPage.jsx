import OrderItems from "../../components/customer/Checkout/OrderItems";
import Promotions from "../../components/customer/Checkout/Promotions";
import PriceSummary from "../../components/customer/Checkout/PriceSummary";
import StoreInfo from "../../components/customer/Checkout/StoreInfo";
import OrderNote from "../../components/customer/Checkout/OrderNote";
import ShippingForm from "../../components/customer/Checkout/ShippingForm";

import OrderStatusTimeline from "../../components/customer/OrderDetail/OrderStatusTimeline";
import PaymentHistory from "../../components/customer/OrderDetail/PaymentHistory";

import { orderDetailData } from "../../hooks/mockOrderDetailData";

export default function OrderDetailPage() {
  const order = orderDetailData;
  const isOnline = order.order_type === "ONLINE";

  return (
    <div className="w-full xl:px-12 px-4 py-8 text-gray-200">
      <h1 className="text-3xl font-bold mb-8">
        Chi tiết đơn hàng #{order.code}
      </h1>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* LEFT */}
        <div className="lg:col-span-7 space-y-8">
          <OrderStatusTimeline
            status={order.status}
            history={order.status_history}
          />

          <OrderItems items={order.items} />

          {isOnline ? (
            <ShippingForm form={order.shipping_address} readOnly />
          ) : (
            <StoreInfo store={order.store} />
          )}

          <OrderNote value={order.note} readOnly />

          <PaymentHistory payments={order.payments} />
        </div>

        {/* RIGHT */}
        <div className="lg:col-span-5">
          <div className="bg-zinc-900 rounded-2xl p-6 sticky top-6 space-y-6">
            <StoreInfo store={order.store} />
            <Promotions promotions={order.promotions} readOnly />
            <PriceSummary order={order} />
          </div>
        </div>
      </div>
    </div>
  );
}