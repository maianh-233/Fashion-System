import OrderItems from "../../components/customer/Checkout/OrderItems";
import PriceSummary from "../../components/customer/Checkout/PriceSummary";
import OrderNote from "../../components/customer/Checkout/OrderNote";
import ShippingForm from "../../components/customer/Checkout/ShippingForm";

import OrderStatusTimeline from "../../components/customer/OrderDetail/OrderStatusTimeline";
import PaymentHistory from "../../components/customer/OrderDetail/PaymentHistory";
import AppliedPromotions from "../../components/customer/OrderDetail/AppliedPromotions";

import { orderDetailData } from "../../hooks/mockOrderDetailData";

export default function OrderDetailPage() {
  const order = orderDetailData;
  const isOnline = order.order_type === "ONLINE";

  return (
    <div className="w-full px-4 py-5 text-gray-200 sm:px-6 sm:py-8 xl:px-12">
      <h1 className="mb-6 break-words text-2xl font-bold sm:mb-8 sm:text-3xl">
        Chi tiết đơn hàng #{order.code}
      </h1>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12 lg:gap-8">
        {/* LEFT */}
        <div className="space-y-6 lg:col-span-7 lg:space-y-8">
          <OrderStatusTimeline
            status={order.status}
            history={order.status_history}
          />

          <OrderItems items={order.items} />

          
          {isOnline && (
            <ShippingForm form={order.shipping_address} readOnly />
          )}
          

          <OrderNote value={order.note} readOnly />

          <PaymentHistory payments={order.payments} />
        </div>

        {/* RIGHT */}
        <div className="lg:col-span-5">
          <div className="space-y-6 lg:sticky lg:top-24">
            <AppliedPromotions
              promotions={order.promotions}
              discountTotal={order.discount_total}
            />
            <div className="rounded-2xl bg-zinc-900 p-4 sm:p-6">
            <PriceSummary order={order} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
