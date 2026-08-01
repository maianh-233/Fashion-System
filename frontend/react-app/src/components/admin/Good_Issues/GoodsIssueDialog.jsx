import { useState } from "react";
import { X } from "lucide-react";

import GoodsIssueBasicInfo from "./GoodsIssueBasicInfo";
import GoodsIssueItemSection from "./GoodsIssueItemSection";
import GoodsIssueSummary from "./GoodsIssueSummary";
import GoodsIssueStatusHistory from "./GoodsIssueStatusHistory";

import ProductPickerDialog from "../../common/ProductPickerDialog";

import {
  mockProducts,
  mockIssueStatusHistories,
} from "../../../hooks/mockProducts";

export default function GoodsIssueDialog({
  open,
  mode = "view",
  issue,
  onClose,
  onSave,
  onChange,
}) {
  const [openProductDialog, setOpenProductDialog] =
    useState(false);

  if (!open) return null;

  const isView = mode === "view";

  /* ==========================================================
      BASIC
  ========================================================== */

  const handleChange = (field, value) => {
    onChange?.({
      ...issue,
      [field]: value,
    });
  };

  const handleNestedChange = (section, value) => {
    onChange?.({
      ...issue,
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

    return {
      totalQuantity,
    };
  };

  /* ==========================================================
      ITEMS
  ========================================================== */

  const handleItemsChange = (items) => {
    const summary = calculateSummary(items);

    onChange?.({
      ...issue,
      items,
      ...summary,
    });
  };

  const handleAddProduct = (newItem) => {
    const existed = issue.items.findIndex(
      (item) => item.variantId === newItem.variantId
    );

    let items = [];

    if (existed >= 0) {
      items = [...issue.items];

      items[existed].quantity += newItem.quantity;
    } else {
      items = [...issue.items, newItem];
    }

    handleItemsChange(items);

    setOpenProductDialog(false);
  };

  const handleRemoveItem = (variantId) => {
    const items = issue.items.filter(
      (item) => item.variantId !== variantId
    );

    handleItemsChange(items);
  };

  const handleUpdateItem = (
    variantId,
    field,
    value
  ) => {
    const items = issue.items.map((item) => {
      if (item.variantId !== variantId)
        return item;

      return {
        ...item,
        [field]: value,
      };
    });

    handleItemsChange(items);
  };

  /* ==========================================================
      STATUS
  ========================================================== */

  const handleApprove = () => {
    handleChange("status", "APPROVED");
  };

  const handleIssue = () => {
    handleChange("status", "ISSUED");
  };

  const handleCancel = () => {
    handleChange("status", "CANCELLED");
  };

  /* ==========================================================
      SUMMARY DATA
  ========================================================== */

  const summary = {
    totalQuantity: issue.totalQuantity,
  };

  // ===== JSX sẽ ở Phần 2 =====
    return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-5">
        <div className="flex h-[90vh] w-full max-w-7xl flex-col overflow-hidden rounded-2xl border border-zinc-700 bg-[#161616] shadow-2xl">
          {/* ================= HEADER ================= */}

          <div className="flex items-center justify-between border-b border-zinc-700 px-7 py-5">
            <div>
              <h2 className="text-2xl font-bold text-orange-400">
                {isView
                  ? "Chi tiết phiếu xuất kho"
                  : "Tạo phiếu xuất kho"}
              </h2>

              <p className="mt-1 text-sm text-zinc-400">
                {isView
                  ? "Xem thông tin phiếu xuất kho"
                  : "Xuất hàng khỏi kho"}
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
            <GoodsIssueBasicInfo
              mode={mode}
              issue={issue}
              onChange={handleChange}
            />



            <GoodsIssueItemSection
              mode={mode}
              items={issue.items}
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

            <GoodsIssueSummary
              summary={summary}
            />

            {isView && (
              <GoodsIssueStatusHistory
                histories={
                  mockIssueStatusHistories
                }
              />
            )}
          </div>

          {/* ================= FOOTER ================= */}

          <div className="flex items-center justify-between border-t border-zinc-700 bg-[#1a1a1a] px-7 py-5">
            <div className="flex gap-3">
              {!isView &&
                issue.status ===
                  "PENDING" && (
                  <button
                    onClick={
                      handleApprove
                    }
                    className="rounded-lg bg-blue-600 px-5 py-2 text-white hover:bg-blue-700"
                  >
                    Duyệt phiếu
                  </button>
                )}

              {!isView &&
                issue.status ===
                  "APPROVED" && (
                  <button
                    onClick={handleIssue}
                    className="rounded-lg bg-green-600 px-5 py-2 text-white hover:bg-green-700"
                  >
                    Xuất kho
                  </button>
                )}

              {!isView &&
                issue.status !==
                  "ISSUED" &&
                issue.status !==
                  "CANCELLED" && (
                  <button
                    onClick={handleCancel}
                    className="rounded-lg bg-red-600 px-5 py-2 text-white hover:bg-red-700"
                  >
                    Hủy phiếu
                  </button>
                )}
            </div>

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
                    onSave?.(issue)
                  }
                  className="rounded-lg bg-orange-500 px-6 py-2 font-medium text-white hover:bg-orange-600"
                >
                  Lưu phiếu xuất
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
        mode="issue"
        onClose={() =>
          setOpenProductDialog(false)
        }
        onAdd={handleAddProduct}
      />
    </>
  );
}