import Button from "../../common/Button";
import ProductBasicInfo from "./ProductBasicInfo";
import ProductImageSection from "./ProductImageSection";
import ProductMetaInfo from "./ProductMetaInfo";
import ProductAttributeSection from "./ProductAttributeSection";
import ProductTagSection from "./ProductTagSection";
import { mockAttributes,mockTags } from "../../../hooks/mockAttributes"
import AdminDialog, {
  AdminDialogBody,
  AdminDialogFooter,
  AdminDialogHeader,
} from "../common/AdminDialog";

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
    <AdminDialog open={open} onClose={onClose} size="lg">
        <AdminDialogHeader
          title={
            (isView && "Chi tiết sản phẩm") ||
            (isEdit && "Chỉnh sửa sản phẩm") ||
            "Thêm sản phẩm"
          }
          onClose={onClose}
        />

        {/* BODY */}
        <AdminDialogBody className="space-y-8">


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
            // tags={product?.tags || []}
            tags={mockTags || []}
          />
        </AdminDialogBody>

        {/* FOOTER */}
        {!isView && (
          <AdminDialogFooter>
            <Button
              onClick={onClose}
              className="px-4 py-2 rounded bg-gray-700 hover:bg-gray-600"
            >
              Hủy
            </Button>
            <Button
              onClick={onSubmit}
              className="px-6 py-2 rounded bg-orange-500 hover:bg-orange-600 text-black font-semibold"
            >
              {isCreate ? "Thêm mới" : "Lưu thay đổi"}
            </Button>
          </AdminDialogFooter>
        )}
    </AdminDialog>
  );
}
