import { useState } from "react";
import { X } from "lucide-react";

import OrderBasicInfo from "./OrderBasicInfo";
import OrderCustomerSection from "./OrderCustomerSection";
import OrderItemSection from "./OrderItemSection";
import OrderAddressSection from "./OrderAddressSection";
import OrderPaymentSection from "./OrderPaymentSection";
import OrderPromotionSection from "./OrderPromotionSection";
import OrderShipmentSection from "./OrderShipmentSection";
import OrderStatusHistory from "./OrderStatusHistory";
import ProductPickerDialog from "../../common/ProductPickerDialog";
import PromotionPickerDialog from "./PromotionPickerDialog";  

import { mockProducts ,mockPromotions, mockStatusHistories} from "../../../hooks/mockProducts";

export default function OrderDialog({
  open,
  mode = "view",
  order,
  onClose,
  onSave,
  onChange,
}) {
  const [openProductDialog, setOpenProductDialog] = useState(false);
  const [openPromotionDialog, setOpenPromotionDialog] = useState(false);
  const handleAddPromotion = (promotion) => {
    handlePromotionChange([
      ...order.promotions,
      promotion,
    ]);

    setOpenPromotionDialog(false);
  };

  if (!open) return null;

  const isView = mode === "view";

  /* ---------------- BASIC ---------------- */

  const handleChange = (field, value) => {
    onChange?.({
      ...order,
      [field]: value,
    });
  };

  const handleNestedChange = (section, value) => {
    onChange?.({
      ...order,
      [section]: value,
    });
  };

  /* ---------------- SUMMARY ---------------- */

  const calculateSummary = (
    items,
    promotions = order.promotions
  ) => {
    const subtotal = items.reduce(
      (sum, item) => sum + Number(item.total),
      0
    );

    const discount = promotions.reduce(
      (sum, p) => sum + Number(p.discountAmount || 0),
      0
    );

    const tax = Number(order.tax || 0);

    const shippingFee =
      order.orderType === "OFFLINE"
        ? 0
        : Number(order.shippingFee || 0);

    return {
      subtotal,
      discount,
      tax,
      shippingFee,
      total:
        subtotal -
        discount +
        tax +
        shippingFee,
    };
  };

  /* ---------------- ITEMS ---------------- */

  const handleItemsChange = (items) => {
    const summary = calculateSummary(items);

    onChange?.({
      ...order,
      items,
      ...summary,
    });
  };

  const handleAddProduct = (newItem) => {
    const existed = order.items.findIndex(
      (item) =>
        item.variantId === newItem.variantId
    );

    let items = [];

    if (existed >= 0) {
      items = [...order.items];

      items[existed].quantity += newItem.quantity;

      items[existed].total =
        items[existed].quantity *
        items[existed].price;
    } else {
      items = [...order.items, newItem];
    }

    handleItemsChange(items);

    setOpenProductDialog(false);
  };

  /* ---------------- PROMOTION ---------------- */

  const handlePromotionChange = (
    promotions
  ) => {
    const summary = calculateSummary(
      order.items,
      promotions
    );

    onChange?.({
      ...order,
      promotions,
      ...summary,
    });
  };

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-5">

        <div className="w-full max-w-7xl h-[90vh] overflow-hidden rounded-2xl bg-[#161616] border border-zinc-700 shadow-2xl flex flex-col">

          {/* HEADER */}

          <div className="flex items-center justify-between border-b border-zinc-700 px-7 py-5">

            <div>
              <h2 className="text-2xl font-bold text-orange-400">
                {isView
                  ? "Chi tiết đơn hàng"
                  : "Tạo đơn hàng Offline"}
              </h2>

              <p className="mt-1 text-sm text-zinc-400">
                {isView
                  ? "Xem thông tin đơn hàng"
                  : "Tạo đơn hàng tại cửa hàng"}
              </p>
            </div>

            <button
              onClick={onClose}
              className="rounded-lg p-2 hover:bg-zinc-800"
            >
              <X
                size={22}
                className="text-zinc-300"
              />
            </button>

          </div>

          {/* BODY */}

          <div className="flex-1 overflow-y-auto space-y-8 p-7">

            <OrderBasicInfo
              mode={mode}
              order={order}
              onChange={handleChange}
            />

            <OrderCustomerSection
              mode={mode}
              customer={order.customer}
              onChange={(value) =>
                handleNestedChange(
                  "customer",
                  value
                )
              }
            />

            <OrderItemSection
              mode={mode}
              items={order.items}
             
              summary={{
                subtotal: order.subtotal,
                discount: order.discount,
                tax: order.tax,
                shippingFee:
                  order.shippingFee,
                total: order.total,
              }}
              onChange={handleItemsChange}
              onAddProduct={() =>
                setOpenProductDialog(true)
              }
            />

            <OrderPromotionSection
              mode={mode}
              promotions={order.promotions}
              onChange={
                handlePromotionChange
              }
              onAddPromotion={() =>
                setOpenPromotionDialog(true)
              }
            />

            <OrderAddressSection
              mode={mode}
              orderType={order.orderType}
              address={order.address}
              onChange={(value) =>
                handleNestedChange(
                  "address",
                  value
                )
              }
            />

            <OrderPaymentSection
              mode={mode}
              paymentStatus={
                order.paymentStatus
              }
              paymentMethod={
                order.paymentMethod
              }
              onStatusChange={(value) =>
                handleChange(
                  "paymentStatus",
                  value
                )
              }
              onMethodChange={(value) =>
                handleChange(
                  "paymentMethod",
                  value
                )
              }
            />

            {order.orderType !==
              "OFFLINE" && (
              <OrderShipmentSection
                mode={mode}
                shipment={order.shipment}
                onChange={(value) =>
                  handleNestedChange(
                    "shipment",
                    value
                  )
                }
              />
            )}

            {isView && (
              <OrderStatusHistory
                histories={
                  mockStatusHistories
                }
              />
            )}

          </div>

          {/* FOOTER */}

          <div className="flex justify-end gap-3 border-t border-zinc-700 bg-[#1a1a1a] px-7 py-5">

            <button
              onClick={onClose}
              className="rounded-lg border border-zinc-700 px-5 py-2 text-zinc-300 hover:bg-zinc-800"
            >
              {isView ? "Đóng" : "Hủy"}
            </button>

            {!isView && (
              <button
                onClick={() =>
                  onSave?.(order)
                }
                className="rounded-lg bg-orange-500 px-6 py-2 font-medium text-white hover:bg-orange-600"
              >
                Tạo đơn hàng
              </button>
            )}

          </div>
        </div>
      </div>

      {/* PRODUCT PICKER */}

      <ProductPickerDialog
        open={openProductDialog}
        products={mockProducts}
        onClose={() =>
          setOpenProductDialog(false)
        }
        onAdd={handleAddProduct}
      />

      <PromotionPickerDialog
        open={openPromotionDialog}
        promotions={mockPromotions}
        selectedPromotions={order.promotions}
        onClose={() => setOpenPromotionDialog(false)}
        onAdd={handleAddPromotion}
      />
    </>
  );
}