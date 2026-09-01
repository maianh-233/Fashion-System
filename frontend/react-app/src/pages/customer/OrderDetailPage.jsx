import OrderItems from "../../components/customer/Checkout/OrderItems";
import PriceSummary from "../../components/customer/Checkout/PriceSummary";
import OrderNote from "../../components/customer/Checkout/OrderNote";
import ShippingForm from "../../components/customer/Checkout/ShippingForm";

import OrderStatusTimeline from "../../components/customer/OrderDetail/OrderStatusTimeline";
import PaymentHistory from "../../components/customer/OrderDetail/PaymentHistory";
import AppliedPromotions from "../../components/customer/OrderDetail/AppliedPromotions";
import ProcessingStore from "../../components/customer/Checkout/ProcessingStore";

import { orderDetailData } from "../../hooks/mockOrderDetailData";
import { ArrowLeft, PackageCheck, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";

export default function OrderDetailPage() {
  const order = orderDetailData;
  const isOnline = order.order_type === "ONLINE";

  return (
    <div className="customer-page order-detail-page min-h-screen w-full text-gray-200">
      <div className="customer-page__wide order-detail-page__inner">
      <header className="order-detail-heading">
        <div>
          <p><Sparkles size={13} /> Order overview</p>
          <h1>Chi tiết đơn hàng <span>#{order.code}</span></h1>
          <small><PackageCheck size={13} /> Theo dõi trạng thái, thanh toán và thông tin giao nhận.</small>
        </div>
        <Link to="/orders"><ArrowLeft size={14} /> Quay lại đơn hàng</Link>
      </header>

      <div className="order-detail-layout">
        {/* LEFT */}
        <div className="order-detail-content">
          <OrderStatusTimeline
            status={order.status}
            history={order.status_history}
          />

          <OrderItems items={order.items} eyebrow="Order items" title="Sản phẩm trong đơn" />

          <ProcessingStore
            selectedStore={order.store}
            readOnly
            pickup={!isOnline}
          />

          
          {isOnline && (
            <ShippingForm form={order.shipping_address} readOnly eyebrow="Delivery details" title="Thông tin giao nhận" />
          )}
          

          <OrderNote value={order.note} readOnly eyebrow="Customer note" title="Ghi chú của đơn hàng" />

          <PaymentHistory payments={order.payments} />
        </div>

        {/* RIGHT */}
        <aside className="order-detail-summary">
          <div className="order-detail-summary__heading">
            <p>Payment record</p>
            <h2>Tổng quan thanh toán</h2>
            <span>{order.payments.length} giao dịch được ghi nhận</span>
          </div>
            <AppliedPromotions
              promotions={order.promotions}
              discountTotal={order.discount_total}
            />
            <PriceSummary order={order} readOnly />
        </aside>
      </div>
      </div>
    </div>
  );
}
