import { useCallback, useState } from "react";
import { Link, Navigate, useSearchParams } from "react-router-dom";
import { ArrowLeft, CreditCard, ExternalLink, MapPinned, MessageCircle, PackageCheck, Sparkles, Store, Wallet, XCircle } from "lucide-react";
import Button from "../../components/common/Button";
import { useSystemNotification } from "../../components/common/SystemNotification";
import OrderItems from "../../components/customer/Checkout/OrderItems";
import PriceSummary from "../../components/customer/Checkout/PriceSummary";
import OrderNote from "../../components/customer/Checkout/OrderNote";
import OrderStatusTimeline from "../../components/customer/OrderDetail/OrderStatusTimeline";
import PaymentHistory from "../../components/customer/OrderDetail/PaymentHistory";
import AppliedPromotions from "../../components/customer/OrderDetail/AppliedPromotions";
import CancelOrderDialog from "../../components/customer/Order/CancelOrderDialog";
import ChatModal from "../../components/customer/Chat/ChatModal";
import DetailState from "../../components/customer/DetailState";
import { customerOrderApi } from "../../api/customerApi";
import { useCustomerSession } from "../../hooks/useCustomerSession";
import { useStorefrontResource } from "../../hooks/useStorefrontResource";
import { PAYMENT_METHOD_LABELS, PAYMENT_STATUS_LABELS, paymentCountdown } from "./customerOrderLogic";

const loadOrder = (id, options) => customerOrderApi.detail(id, options);

/** Chi tiết đơn của khách: {@code /orderdetail?id=<uuid>}. */
export default function OrderDetailPage() {
  const { loggedIn } = useCustomerSession();
  const notification = useSystemNotification();
  const [searchParams] = useSearchParams();
  const id = searchParams.get("id");
  const [version, setVersion] = useState(0);
  const load = useCallback((orderId, options) => loadOrder(orderId, options), [version]); // eslint-disable-line react-hooks/exhaustive-deps
  const { data: order, loading, error } = useStorefrontResource(id, load, "Đơn hàng không tồn tại.");
  const [busy, setBusy] = useState(null);
  const [cancelling, setCancelling] = useState(false);
  const [chatting, setChatting] = useState(false);

  if (!loggedIn) return <Navigate to={`/customerlogin?redirect=${encodeURIComponent(`/orderdetail?id=${id || ""}`)}`} replace />;

  const run = async (key, action) => {
    setBusy(key);
    try {
      await action();
    } catch (failure) {
      notification.error(failure.message);
    } finally {
      setBusy(null);
    }
  };
  const pay = () => run("pay", async () => {
    const payment = await customerOrderApi.pay(order.id);
    window.location.assign(payment.payUrl);
  });
  const switchToCod = () => run("cod", async () => {
    if (!await notification.confirm("Đổi sang thanh toán khi nhận hàng? Cửa hàng sẽ bắt đầu xử lý đơn ngay.", { confirmText: "Đổi sang COD" })) return;
    await customerOrderApi.switchToCod(order.id);
    notification.success("Đơn đã chuyển sang thanh toán khi nhận hàng.");
    setVersion((value) => value + 1);
  });

  return (
    <div className="customer-page order-detail-page min-h-screen w-full text-gray-200">
      <div className="customer-page__wide order-detail-page__inner">
        <DetailState loading={loading} error={error} icon={PackageCheck} backTo="/orders" backLabel="Xem đơn hàng của tôi" />
        {order && <>
          <header className="order-detail-heading">
            <div>
              <p><Sparkles size={13} /> Order overview</p>
              <h1>Chi tiết đơn hàng <span>#{order.orderCode}</span></h1>
              <small><PackageCheck size={13} /> {order.statusLabel} · {PAYMENT_STATUS_LABELS[order.paymentStatus] || order.paymentStatus}</small>
            </div>
            <Link to="/orders"><ArrowLeft size={14} /> Quay lại đơn hàng</Link>
          </header>

          {order.canPay && (
            <div className="order-detail-payment-due">
              <CreditCard size={16} />
              <span>Đơn đang giữ hàng cho bạn. Vui lòng thanh toán {PAYMENT_METHOD_LABELS[order.paymentMethod]} trong <strong>{paymentCountdown(order.paymentDueAt)}</strong>, quá hạn đơn sẽ tự hủy.</span>
              <Button variant="unstyled" className="checkout-primary-button" loading={busy === "pay"} onClick={pay}>Thanh toán ngay</Button>
              {order.canSwitchToCod && (
                <Button variant="unstyled" className="checkout-secondary-button" loading={busy === "cod"} onClick={switchToCod}>
                  <Wallet size={14} /> Đổi sang COD
                </Button>
              )}
            </div>
          )}

          <div className="order-detail-layout">
            <div className="order-detail-content">
              <OrderStatusTimeline status={order.status} history={order.history} cancelReason={order.cancelReason} />

              {order.parts.map((part, index) => (
                <div key={part.id} className="order-detail-part">
                  <OrderItems
                    eyebrow={order.split ? `Phần ${index + 1} · ${part.orderCode} · ${part.statusLabel}` : "Order items"}
                    title={order.split ? part.storeName : "Sản phẩm trong đơn"}
                    items={part.items.map((item) => ({
                      key: item.id, name: item.productName, image: item.imageUrl, color: item.color, size: item.size,
                      price: item.quantity ? Number(item.total) / item.quantity : item.price, quantity: item.quantity, total: item.total,
                      note: item.promotionCode ? `Ưu đãi ${item.promotionCode}` : null,
                    }))}
                  />
                  <p className="order-detail-part__store">
                    <Store size={13} /> Giao từ {part.storeName}
                    {part.deliveryStatus && <> · {part.deliveryStatus}</>}
                    {part.trackingUrl && (
                      <a href={part.trackingUrl} target="_blank" rel="noreferrer"><ExternalLink size={12} /> Theo dõi vận chuyển</a>
                    )}
                  </p>
                </div>
              ))}

              {order.shippingAddress && (
                <section className="checkout-section">
                  <div className="checkout-section__heading">
                    <span><MapPinned size={17} /></span>
                    <div><small>Delivery details</small><h2>Thông tin giao nhận</h2></div>
                  </div>
                  <p className="order-detail-address">
                    <strong>{order.shippingAddress.receiverName}</strong> · {order.shippingAddress.receiverPhone}<br />
                    {[order.shippingAddress.addressLine, order.shippingAddress.ward, order.shippingAddress.district, order.shippingAddress.province].filter(Boolean).join(", ")}
                  </p>
                </section>
              )}

              {order.note && <OrderNote value={order.note} readOnly eyebrow="Customer note" title="Ghi chú của đơn hàng" />}
              <PaymentHistory payments={order.payments} method={order.paymentMethod} />
            </div>

            <aside className="order-detail-summary">
              <div className="order-detail-summary__heading">
                <p>Payment record</p>
                <h2>Tổng quan thanh toán</h2>
                <span>{PAYMENT_METHOD_LABELS[order.paymentMethod] || order.paymentMethod}</span>
              </div>
              <AppliedPromotions promotions={order.promotions} discountTotal={order.discountTotal} />
              <PriceSummary subtotal={order.subtotal} discount={order.discountTotal} shippingFee={order.shippingFee}
                total={order.total} readOnly>
                <div className="order-detail-actions">
                  <Button variant="unstyled" className="checkout-secondary-button" onClick={() => setChatting(true)}>
                    <MessageCircle size={14} /> Hỗ trợ đơn hàng
                  </Button>
                  {order.canCancel && (
                    <Button variant="unstyled" className="checkout-secondary-button order-detail-actions__danger" onClick={() => setCancelling(true)}>
                      <XCircle size={14} /> Hủy đơn
                    </Button>
                  )}
                </div>
              </PriceSummary>
            </aside>
          </div>

          {chatting && <ChatModal order={order} onClose={() => setChatting(false)} />}
          {cancelling && (
            <CancelOrderDialog order={order} onClose={() => setCancelling(false)} onCancelled={() => {
              setCancelling(false);
              notification.success(`Đã hủy đơn #${order.orderCode}.`);
              setVersion((value) => value + 1);
            }} />
          )}
        </>}
      </div>
    </div>
  );
}
