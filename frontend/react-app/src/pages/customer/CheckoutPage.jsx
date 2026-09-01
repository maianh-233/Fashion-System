import { useState } from "react";
import { ArrowLeft, LockKeyhole, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";
import {
  orderData as mockOrderData,
  savedAddresses,
  stores,
} from "../../hooks/mockCheckoutData";

import OrderItems from "../../components/customer/Checkout/OrderItems";
import SavedAddresses from "../../components/customer/Checkout/SavedAddresses";
import ShippingForm from "../../components/customer/Checkout/ShippingForm";
import Promotions from "../../components/customer/Checkout/Promotions";
import PriceSummary from "../../components/customer/Checkout/PriceSummary";


import OrderTypeSelector from "../../components/customer/Checkout/OrderTypeSelector";
import PaymentMethodSelector from "../../components/customer/Checkout/PaymentMethodSelector";
import OrderNote from "../../components/customer/Checkout/OrderNote";
import ProcessingStore from "../../components/customer/Checkout/ProcessingStore";

export default function CheckoutPage() {
  /* ========= ORDER ========= */
  const [order, setOrder] = useState(mockOrderData);

  /* ========= TYPE & PAYMENT ========= */
  const [orderType, setOrderType] = useState("ONLINE"); // ONLINE | PICKUP
  const [paymentMethod, setPaymentMethod] = useState("COD"); // COD | VNPAY
  const [processingStore, setProcessingStore] = useState(stores[0]);

  /* ========= NOTE ========= */
  const [note, setNote] = useState("");

  /* ========= SHIPPING FORM ========= */
  const defaultAddress =
    savedAddresses.find(a => a.is_default) || {
      receiver_name: "",
      receiver_phone: "",
      province: "",
      district: "",
      ward: "",
      address_line: "",
      latitude: null,
      longitude: null,
    };

  const [shippingForm, setShippingForm] = useState(defaultAddress);

  const handleOrderTypeChange = (value) => {
    setOrderType(value);
    if (value === "PICKUP") setShippingForm(defaultAddress);
  };

  /* ========= UI ========= */
  return (
    <div className="customer-page checkout-page min-h-screen w-full text-gray-200">
      <div className="customer-page__wide checkout-page__inner">
      <header className="checkout-page__header">
        <div>
          <p><Sparkles size={13} /> Secure checkout</p>
          <h1>Hoàn tất đơn hàng</h1>
          <span><LockKeyhole size={13} /> Thông tin của bạn được bảo mật trong suốt quá trình thanh toán.</span>
        </div>
        <Link to="/carts"><ArrowLeft size={14} /> Quay lại giỏ hàng</Link>
      </header>

      <div className="checkout-page__layout">
        {/* ================= LEFT ================= */}
        <div className="checkout-page__content">
          {/* DANH SÁCH SẢN PHẨM */}
          <OrderItems items={order.items} />

          {/* LOẠI ĐƠN */}
          <OrderTypeSelector
            value={orderType}
            onChange={handleOrderTypeChange}
          />

          <ProcessingStore
            stores={stores}
            selectedStore={processingStore}
            onSelect={setProcessingStore}
            pickup={orderType === "PICKUP"}
          />

          {/* ========== ONLINE ========== */}
          {orderType === "ONLINE" && (
            <>
              <SavedAddresses
                addresses={savedAddresses}
                selectedId={shippingForm.id}
                onSelect={addr => setShippingForm(addr)}
              />

              <ShippingForm
                form={shippingForm}
                setForm={setShippingForm}
              />


            </>
          )}




          {/* NOTE */}
          <OrderNote
            value={note}
            onChange={setNote}
          />

          {/* THANH TOÁN */}
          <PaymentMethodSelector
            value={paymentMethod}
            onChange={setPaymentMethod}
          />
        </div>

        {/* ================= RIGHT ================= */}
        <aside className="checkout-summary">
          <div className="checkout-summary__heading">
            <p>Order review</p>
            <h2>Đơn hàng của bạn</h2>
            <span>{order.items.length} sản phẩm</span>
          </div>

            <Promotions
              promotions={order.promotions}
              onApply={(discount) =>
                setOrder(prev => ({
                  ...prev,
                  discount_total: prev.discount_total + discount,
                }))
              }
            />

            <PriceSummary order={order} />
        </aside>
      </div>
      </div>
    </div>
  );
}
