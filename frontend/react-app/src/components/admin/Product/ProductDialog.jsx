import { X } from "lucide-react";
import ProductBasicInfo from "./ProductBasicInfo";
import ProductImageSection from "./ProductImageSection";
import ProductMetaInfo from "./ProductMetaInfo";
import ProductAttributeSection from "./ProductAttributeSection";
import ProductTagSection from "./ProductTagSection";
import { mockAttributes } from "../../../hooks/mockAttributes"

export default function ProductDialog({
  open,
  mode = "view", // view | edit | create
  product,
  onClose,
  onSubmit,
}) {
  if (!open) return null;

  const isView = mode === "view";
  const isEdit = mode === "edit";
  const isCreate = mode === "create";

  return (
<div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50">
  <div className="bg-[#121212] w-[1000px] max-h-[90vh] rounded-xl text-white flex flex-col">

        {/* HEADER */}
        <div className="flex justify-between items-center px-6 py-4 border-b border-gray-800 ">
          <h2 className="text-xl font-semibold text-orange-400">
            {isView && "Chi tiết sản phẩm"}
            {isEdit && "Chỉnh sửa sản phẩm"}
            {isCreate && "Thêm sản phẩm"}
          </h2>
          <button onClick={onClose}>
            <X />
          </button>
        </div>

        {/* BODY */}
        <div className="flex-1 overflow-y-scroll scrollbar-hide p-6 space-y-8">


          <ProductImageSection
            mode={mode}
            imageUrl={product?.imageUrl}
          />

          <ProductBasicInfo
            mode={mode}
            product={product}
          />

          {!isCreate && (
            <ProductMetaInfo
              createdAt={product?.createdAt}
              updatedAt={product?.updatedAt}
            />
          )}

          <ProductAttributeSection
            mode={mode}
            // attributes={product?.attributes || []}
            attributes={mockAttributes || []}
          />

          <ProductTagSection
            mode={mode}
            tags={product?.tags || []}
          />
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