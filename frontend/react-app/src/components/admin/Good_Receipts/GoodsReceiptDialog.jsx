import Button from "../../common/Button";
import { useState } from "react";

import GoodsReceiptBasicInfo from "./GoodsReceiptBasicInfo";
import GoodsReceiptSupplierSection from "./GoodsReceiptSupplierSection";
import GoodsReceiptItemSection from "./GoodsReceiptItemSection";
import GoodsReceiptSummary from "./GoodsReceiptSummary";
import GoodsReceiptStatusHistory from "./GoodsReceiptStatusHistory";

import ProductPickerDialog from "../../common/ProductPickerDialog";
import AdminDialog, {
  AdminDialogBody,
  AdminDialogFooter,
  AdminDialogHeader,
} from "../common/AdminDialog";
import SupplierPickerDialog from "./SupplierPickerDialog";



import {
  mockProducts,
  mockReceiptStatusHistories,
  mockSuppliers,
} from "../../../hooks/mockProducts";

export default function GoodsReceiptDialog({
  open,
  mode = "view",
  receipt,
  onClose,
  onSave,
  onChange,
}) {
  const [openProductDialog, setOpenProductDialog] = useState(false);
  const [openSupplierDialog, setOpenSupplierDialog] =useState(false);


  if (!open) return null;

  const isView = mode === "view";

  /* ==========================================================
      BASIC
  ========================================================== */
  const handleSelectSupplier = (supplier) => {
    handleNestedChange("supplier", supplier);
    setOpenSupplierDialog(false);
  };

  const handleChange = (field, value) => {
    onChange?.({
      ...receipt,
      [field]: value,
    });
  };

  const handleNestedChange = (section, value) => {
    onChange?.({
      ...receipt,
      [section]: value,
    });
  };

  /* ==========================================================
      SUMMARY
  ========================================================== */

  const calculateSummary = (items) => {
    const totalQuantity = items.reduce(
      (sum, item) => sum + Number(item.quantity || 0),
      0
    );

    const totalAmount = items.reduce(
      (sum, item) => sum + Number(item.total || 0),
      0
    );

    return {
      totalQuantity,
      totalAmount,
    };
  };

  /* ==========================================================
      ITEMS
  ========================================================== */

  const handleItemsChange = (items) => {
    const summary = calculateSummary(items);

    onChange?.({
      ...receipt,
      items,
      ...summary,
    });
  };

  const handleAddProduct = (newItem) => {
    const existed = receipt.items.findIndex(
      (item) => item.variantId === newItem.variantId
    );

    let items = [];

    if (existed >= 0) {
      items = [...receipt.items];

      items[existed].quantity += newItem.quantity;

      items[existed].total =
        items[existed].quantity * items[existed].costPrice;
    } else {
      items = [...receipt.items, newItem];
    }

    handleItemsChange(items);

    setOpenProductDialog(false);
  };

  const handleRemoveItem = (variantId) => {
    const items = receipt.items.filter(
      (item) => item.variantId !== variantId
    );

    handleItemsChange(items);
  };

  const handleUpdateItem = (variantId, field, value) => {
    const items = receipt.items.map((item) => {
      if (item.variantId !== variantId) return item;

      const updated = {
        ...item,
        [field]: value,
      };

      updated.total =
        Number(updated.costPrice || 0) *
        Number(updated.quantity || 0);

      return updated;
    });

    handleItemsChange(items);
  };

  /* ==========================================================
      STATUS
  ========================================================== */

  /* ==========================================================
      SUMMARY DATA
  ========================================================== */

  const summary = {
    totalQuantity: receipt.totalQuantity,
    totalAmount: receipt.totalAmount,
  };

  // ===== JSX (Header + Body + Footer + ProductPickerDialog)
  // sẽ ở Phần 1B

    return (
    <>
      <AdminDialog open onClose={onClose} size="full" className="h-[90vh]">
          <AdminDialogHeader
            title={isView ? "Chi tiết phiếu nhập kho" : "Tạo phiếu nhập kho"}
            description={isView ? "Xem thông tin phiếu nhập" : "Nhập hàng từ nhà cung cấp"}
            onClose={onClose}
          />

          {/* ================= BODY ================= */}

          <AdminDialogBody className="space-y-8">
            <GoodsReceiptBasicInfo
              mode={mode}
              receipt={receipt}
              onChange={handleChange}
            />

            <GoodsReceiptSupplierSection
              mode={mode}
              supplier={receipt.supplier}
              onChange={(value) =>
                handleNestedChange("supplier", value)
              }
              onSelectSupplier={() =>
                setOpenSupplierDialog(true)
              }
            />

            <GoodsReceiptItemSection
              mode={mode}
              items={receipt.items}
              summary={summary}
              onChange={handleItemsChange}
              onUpdateItem={
                handleUpdateItem
              }
              onRemoveItem={
                handleRemoveItem
              }
              onAddProduct={() =>
                setOpenProductDialog(true)
              }
            />

            <GoodsReceiptSummary
              summary={summary}
            />

            {isView && (
              <GoodsReceiptStatusHistory
                histories={
                  mockReceiptStatusHistories
                }
              />
            )}
          </AdminDialogBody>

          {/* ================= FOOTER ================= */}

          <AdminDialogFooter className="admin-dialog__footer--split">
            {/* <div className="flex gap-3">
              {!isView &&
                receipt.status ===
                  "PENDING" && (
                  <Button
                    onClick={handleApprove}
                    className="rounded-lg bg-blue-600 px-5 py-2 text-white hover:bg-blue-700"
                  >
                    Duyệt phiếu
                  </Button>
                )}

              {!isView &&
                receipt.status ===
                  "APPROVED" && (
                  <Button
                    onClick={handleReceive}
                    className="rounded-lg bg-green-600 px-5 py-2 text-white hover:bg-green-700"
                  >
                    Nhập kho
                  </Button>
                )}

              {!isView &&
                receipt.status !==
                  "CANCELLED" &&
                receipt.status !==
                  "RECEIVED" && (
                  <Button
                    onClick={handleCancel}
                    className="rounded-lg bg-red-600 px-5 py-2 text-white hover:bg-red-700"
                  >
                    Hủy phiếu
                  </Button>
                )}
            </div> */}

            <div className="flex gap-3">
              <Button
                onClick={onClose}
                className="rounded-lg border border-zinc-700 px-5 py-2 text-zinc-300 hover:bg-zinc-800"
              >
                {isView ? "Đóng" : "Hủy"}
              </Button>

              {!isView && (
                <Button
                  onClick={() =>
                    onSave?.(receipt)
                  }
                  className="rounded-lg bg-orange-500 px-6 py-2 font-medium text-white hover:bg-orange-600"
                >
                  Lưu phiếu nhập
                </Button>
              )}
            </div>
          </AdminDialogFooter>
      </AdminDialog>

      {/* ================= PRODUCT PICKER ================= */}

      <ProductPickerDialog
        open={openProductDialog}
        products={mockProducts}
        type="receipt"
        priceField="costPrice"
        onClose={() =>
          setOpenProductDialog(false)
        }
        onAdd={handleAddProduct}
      />

      <SupplierPickerDialog
        open={openSupplierDialog}
        suppliers={mockSuppliers}
        onClose={() => setOpenSupplierDialog(false)}
        onAdd={handleSelectSupplier}
      />

    </>
  );
}
