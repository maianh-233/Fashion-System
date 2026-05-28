// hooks/mockOrderDetailData.js
import { orderData, savedAddresses, store } from "./mockCheckoutData";

export const orderDetailData = {
  id: "ord_001",
  code: "ORD-20260512-001",

  order_type: "ONLINE", // ONLINE | PICKUP
  status: "SHIPPING",

  status_history: [
    { status: "CREATED", time: "2026-05-12T08:00:00" },
    { status: "CONFIRMED", time: "2026-05-12T08:10:00" },
    { status: "SHIPPING", time: "2026-05-12T09:30:00" },
  ],

  ...orderData,

  shipping_address: savedAddresses[0],
  store,

  note: "Giao giờ hành chính, gọi trước khi giao",

  payments: [
    {
      id: "pay_001",
      payment_code: "PAY-20260512-0001",
      method: "VNPAY",
      amount: 1650000,
      status: "PAID",
      transaction_code: "VNPAY_983746283",
      paid_at: "2026-05-12T08:12:00",
      created_at: "2026-05-12T08:11:00",
    },
    {
      id: "pay_002",
      payment_code: "PAY-20260512-0002",
      method: "VNPAY",
      amount: 1650000,
      status: "FAILED",
      transaction_code: "VNPAY_983746999",
      paid_at: null,
      created_at: "2026-05-12T08:05:00",
    },
  ],
};