import Button from "../../common/Button";
import { useState } from "react";

import GoodsIssueBasicInfo from "./GoodsIssueBasicInfo";
import GoodsIssueItemSection from "./GoodsIssueItemSection";
import GoodsIssueSummary from "./GoodsIssueSummary";
import GoodsIssueStatusHistory from "./GoodsIssueStatusHistory";

import ProductPickerDialog from "../../common/ProductPickerDialog";
import AdminDialog, {
  AdminDialogBody,
  AdminDialogFooter,
  AdminDialogHeader,
} from "../common/AdminDialog";

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
      <AdminDialog open onClose={onClose} size="full" className="h-[90vh]">
          <AdminDialogHeader
            title={isView ? "Chi tiết phiếu xuất kho" : "Tạo phiếu xuất kho"}
            description={isView ? "Xem thông tin phiếu xuất kho" : "Xuất hàng khỏi kho"}
            onClose={onClose}
          />

          {/* ================= BODY ================= */}

          <AdminDialogBody className="space-y-8">
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
          </AdminDialogBody>

          {/* ================= FOOTER ================= */}

          <AdminDialogFooter className="admin-dialog__footer--split">
            <div className="flex gap-3">
              {!isView &&
                issue.status ===
                  "PENDING" && (
                  <Button
                    onClick={
                      handleApprove
                    }
                    className="rounded-lg bg-blue-600 px-5 py-2 text-white hover:bg-blue-700"
                  >
                    Duyệt phiếu
                  </Button>
                )}

              {!isView &&
                issue.status ===
                  "APPROVED" && (
                  <Button
                    onClick={handleIssue}
                    className="rounded-lg bg-green-600 px-5 py-2 text-white hover:bg-green-700"
                  >
                    Xuất kho
                  </Button>
                )}

              {!isView &&
                issue.status !==
                  "ISSUED" &&
                issue.status !==
                  "CANCELLED" && (
                  <Button
                    onClick={handleCancel}
                    className="rounded-lg bg-red-600 px-5 py-2 text-white hover:bg-red-700"
                  >
                    Hủy phiếu
                  </Button>
                )}
            </div>

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
                    onSave?.(issue)
                  }
                  className="rounded-lg bg-orange-500 px-6 py-2 font-medium text-white hover:bg-orange-600"
                >
                  Lưu phiếu xuất
                </Button>
              )}
            </div>
          </AdminDialogFooter>
      </AdminDialog>

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
