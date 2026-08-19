import Button from "../../common/Button";
import { useState } from "react";

import OrderBasicInfo from "./OrderBasicInfo";
import OrderCustomerSection from "./OrderCustomerSection";
import OrderItemSection from "./OrderItemSection";
import OrderAddressSection from "./OrderAddressSection";
import OrderPaymentSection from "./OrderPaymentSection";
import OrderPromotionSection from "./OrderPromotionSection";
import OrderShipmentSection from "./OrderShipmentSection";
import OrderStatusHistory from "./OrderStatusHistory";
import ProductPickerDialog from "../../common/ProductPickerDialog";
import AdminDialog, {
  AdminDialogBody,
  AdminDialogFooter,
  AdminDialogHeader,
} from "../common/AdminDialog";
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
      <AdminDialog open onClose={onClose} size="full" className="h-[90vh]">
          <AdminDialogHeader
            title={isView ? "Chi tiết đơn hàng" : "Tạo đơn hàng Offline"}
            description={isView ? "Xem thông tin đơn hàng" : "Tạo đơn hàng tại cửa hàng"}
            onClose={onClose}
          />

          {/* BODY */}

          <AdminDialogBody className="space-y-8">

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

          </AdminDialogBody>

          {/* FOOTER */}

          <AdminDialogFooter>

            <Button
              onClick={onClose}
              className="rounded-lg border border-zinc-700 px-5 py-2 text-zinc-300 hover:bg-zinc-800"
            >
              {isView ? "Đóng" : "Hủy"}
            </Button>

            {!isView && (
              <Button
                onClick={() =>
                  onSave?.(order)
                }
                className="rounded-lg bg-orange-500 px-6 py-2 font-medium text-white hover:bg-orange-600"
              >
                Tạo đơn hàng
              </Button>
            )}

          </AdminDialogFooter>
      </AdminDialog>

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
