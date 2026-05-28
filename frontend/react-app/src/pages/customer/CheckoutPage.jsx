import { useState, useEffect } from "react";
import {
  orderData as mockOrderData,
  savedAddresses,
  store,
} from "../../hooks/mockCheckoutData";

import OrderItems from "../../components/customer/Checkout/OrderItems";
import SavedAddresses from "../../components/customer/Checkout/SavedAddresses";
import ShippingForm from "../../components/customer/Checkout/ShippingForm";
import Promotions from "../../components/customer/Checkout/Promotions";
import PriceSummary from "../../components/customer/Checkout/PriceSummary";
import MapSection from "../../components/customer/Checkout/MapSection";
import StoreInfo from "../../components/customer/Checkout/StoreInfo";

import OrderTypeSelector from "../../components/customer/Checkout/OrderTypeSelector";
import PaymentMethodSelector from "../../components/customer/Checkout/PaymentMethodSelector";
import OrderNote from "../../components/customer/Checkout/OrderNote";

export default function CheckoutPage() {
  /* ========= ORDER ========= */
  const [order, setOrder] = useState(mockOrderData);

  /* ========= TYPE & PAYMENT ========= */
  const [orderType, setOrderType] = useState("ONLINE"); // ONLINE | PICKUP
  const [paymentMethod, setPaymentMethod] = useState("COD"); // COD | VNPAY
  const [pickupStore, setPickupStore] = useState(null);

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

  /* ========= SIDE EFFECT ========= */

  // Khi chuyển sang PICKUP → reset địa chỉ
  useEffect(() => {
    if (orderType === "PICKUP") {
      setShippingForm(defaultAddress);
    }
  }, [orderType]);

  /* ========= UI ========= */
  return (
    <div className="w-full xl:px-12 px-4 py-8 text-gray-200">
      <h1 className="text-3xl font-bold text-white mb-8">
        Thanh Toán
      </h1>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* ================= LEFT ================= */}
        <div className="lg:col-span-7 space-y-8">
          {/* DANH SÁCH SẢN PHẨM */}
          <OrderItems items={order.items} />

          {/* LOẠI ĐƠN */}
          <OrderTypeSelector
            value={orderType}
            onChange={setOrderType}
            stores={[store]}
            onSelectStore={setPickupStore}
          />

          {/* ========== ONLINE ========== */}
          {orderType === "ONLINE" && (
            <>
              <SavedAddresses
                addresses={savedAddresses}
                onSelect={addr => setShippingForm(addr)}
              />

              <ShippingForm
                form={shippingForm}
                setForm={setShippingForm}
              />

              <MapSection
                address={shippingForm}
                store={store}
              />
            </>
          )}

          {/* ========== PICKUP ========== */}
          {orderType === "PICKUP" && pickupStore && (
            <StoreInfo store={pickupStore} />
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
        <div className="lg:col-span-5">
          <div className="bg-zinc-900 rounded-2xl p-6 sticky top-6 space-y-6">
            <StoreInfo store={store} />

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
          </div>
        </div>
      </div>
    </div>
  );
}