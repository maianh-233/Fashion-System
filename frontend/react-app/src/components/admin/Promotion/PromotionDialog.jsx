import { X } from "lucide-react";
import PromotionBasicInfo from "./PromotionBasicInfo";
import PromotionDateSection from "./PromotionDateSection";
import PromotionLimitSection from "./PromotionLimitSection";
import PromotionCustomerTier from "./PromotionCustomerTier";
import PromotionMetaInfo from "./PromotionMetaInfo";

export default function PromotionDialog({
  open,
  mode = "view",
  promotion,
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

        {/* HEADER */}
        <div className="flex justify-between items-center px-6 py-4 border-b border-gray-800">
          <h2 className="text-xl font-semibold text-orange-400">
            {isView && "Chi tiết Promotion"}
            {isEdit && "Chỉnh sửa Promotion"}
            {isCreate && "Thêm Promotion"}
          </h2>

          <button onClick={onClose}>
            <X />
          </button>
        </div>

        {/* BODY */}
        <div className="flex-1 overflow-y-auto scrollbar-hide p-6 space-y-8">

          <PromotionBasicInfo
            mode={mode}
            promotion={promotion}
          />

          <PromotionDateSection
            mode={mode}
            promotion={promotion}
          />

          <PromotionLimitSection
            mode={mode}
            promotion={promotion}
          />

          <PromotionCustomerTier
            mode={mode}
            promotion={promotion}
          />

          {!isCreate && (
            <PromotionMetaInfo
              createdAt={promotion?.created_at}
              updatedAt={promotion?.updated_at}
            />
          )}

        </div>

        {/* FOOTER */}

        {!isView && (
          <div className="flex justify-end gap-3 px-6 py-4 border-t border-gray-800">
            <button
              onClick={onClose}
              className="px-4 py-2 rounded bg-gray-700 hover:bg-gray-600"
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