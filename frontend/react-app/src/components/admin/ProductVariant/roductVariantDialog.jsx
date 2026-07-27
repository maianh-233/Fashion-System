import { X } from "lucide-react";
import ProductVariantBasicInfo from "./ProductVariantBasicInfo";
import ProductVariantPrice from "./ProductVariantPrice";
import ProductVariantStatus from "./ProductVariantStatus";
import ProductVariantMetaInfo from "./ProductVariantMetaInfo";
import ProductVariantImageSection from "./ProductVariantImageSection";

export default function ProductVariantDialog({
  open,
  mode = "view",
  variant,
  onClose,
  onSubmit,
}) {
  if (!open) return null;

  const isView = mode === "view";
  const isCreate = mode === "create";
  const isEdit = mode === "edit";

  return (
    <div className="fixed inset-0 bg-black/70 flex justify-center items-center z-50">
      <div className="bg-[#121212] w-[900px] max-h-[90vh] rounded-xl text-white flex flex-col">

        {/* Header */}
        <div className="flex justify-between items-center px-6 py-4 border-b border-gray-800">
          <h2 className="text-xl font-semibold text-orange-400">
            {isView && "Chi tiết biến thể"}
            {isEdit && "Chỉnh sửa biến thể"}
            {isCreate && "Thêm biến thể"}
          </h2>

          <button onClick={onClose}>
            <X />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto scrollbar-hide p-6 space-y-8">
          <ProductVariantImageSection
              mode={mode}
              imageUrl={variant?.imageUrl}
          />

          <ProductVariantBasicInfo
            mode={mode}
            variant={variant}
          />

          <ProductVariantPrice
            mode={mode}
            variant={variant}
          />

          <ProductVariantStatus
            mode={mode}
            variant={variant}
          />

          {!isCreate && (
            <ProductVariantMetaInfo
              createdAt={variant?.created_at}
              updatedAt={variant?.updated_at}
            />
          )}

        </div>

        {/* Footer */}
        {!isView && (
          <div className="flex justify-end gap-3 px-6 py-4 border-t border-gray-800">

            <button
              onClick={onClose}
              className="px-4 py-2 rounded bg-gray-700"
            >
              Hủy
            </button>

            <button
              onClick={onSubmit}
              className="px-6 py-2 rounded bg-orange-500 hover:bg-orange-600 text-black font-semibold"
            >
              {isCreate ? "Thêm mới" : "Lưu thay đổi"}
            </button>

          </div>
        )}

      </div>
    </div>
  );
}