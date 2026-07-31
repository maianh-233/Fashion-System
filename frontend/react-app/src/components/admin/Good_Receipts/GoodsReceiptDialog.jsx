import { useState } from "react";
import { X } from "lucide-react";

import GoodsReceiptBasicInfo from "./GoodsReceiptBasicInfo";
import GoodsReceiptSupplierSection from "./GoodsReceiptSupplierSection";
import GoodsReceiptItemSection from "./GoodsReceiptItemSection";
import GoodsReceiptSummary from "./GoodsReceiptSummary";
import GoodsReceiptStatusHistory from "./GoodsReceiptStatusHistory";

import ProductPickerDialog from "../../common/ProductPickerDialog";
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

  const handleApprove = () => {
    handleChange("status", "APPROVED");
  };

  const handleReceive = () => {
    handleChange("status", "RECEIVED");
  };

  const handleCancel = () => {
    handleChange("status", "CANCELLED");
  };

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
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-5">
        <div className="flex h-[90vh] w-full max-w-7xl flex-col overflow-hidden rounded-2xl border border-zinc-700 bg-[#161616] shadow-2xl">
          {/* ================= HEADER ================= */}

          <div className="flex items-center justify-between border-b border-zinc-700 px-7 py-5">
            <div>
              <h2 className="text-2xl font-bold text-orange-400">
                {isView
                  ? "Chi tiết phiếu nhập kho"
                  : "Tạo phiếu nhập kho"}
              </h2>

              <p className="mt-1 text-sm text-zinc-400">
                {isView
                  ? "Xem thông tin phiếu nhập"
                  : "Nhập hàng từ nhà cung cấp"}
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

          {/* ================= BODY ================= */}

          <div className="flex-1 space-y-8 overflow-y-auto p-7">
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
          </div>

          {/* ================= FOOTER ================= */}

          <div className="flex items-center justify-between border-t border-zinc-700 bg-[#1a1a1a] px-7 py-5">
            {/* <div className="flex gap-3">
              {!isView &&
                receipt.status ===
                  "PENDING" && (
                  <button
                    onClick={handleApprove}
                    className="rounded-lg bg-blue-600 px-5 py-2 text-white hover:bg-blue-700"
                  >
                    Duyệt phiếu
                  </button>
                )}

              {!isView &&
                receipt.status ===
                  "APPROVED" && (
                  <button
                    onClick={handleReceive}
                    className="rounded-lg bg-green-600 px-5 py-2 text-white hover:bg-green-700"
                  >
                    Nhập kho
                  </button>
                )}

              {!isView &&
                receipt.status !==
                  "CANCELLED" &&
                receipt.status !==
                  "RECEIVED" && (
                  <button
                    onClick={handleCancel}
                    className="rounded-lg bg-red-600 px-5 py-2 text-white hover:bg-red-700"
                  >
                    Hủy phiếu
                  </button>
                )}
            </div> */}

            <div className="flex gap-3">
              <button
                onClick={onClose}
                className="rounded-lg border border-zinc-700 px-5 py-2 text-zinc-300 hover:bg-zinc-800"
              >
                {isView ? "Đóng" : "Hủy"}
              </button>

              {!isView && (
                <button
                  onClick={() =>
                    onSave?.(receipt)
                  }
                  className="rounded-lg bg-orange-500 px-6 py-2 font-medium text-white hover:bg-orange-600"
                >
                  Lưu phiếu nhập
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

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