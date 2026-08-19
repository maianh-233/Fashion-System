import Button from "../../common/Button";
import ProductVariantBasicInfo from "./ProductVariantBasicInfo";
import ProductVariantPrice from "./ProductVariantPrice";
import ProductVariantStatus from "./ProductVariantStatus";
import ProductVariantMetaInfo from "./ProductVariantMetaInfo";
import ProductVariantImageSection from "./ProductVariantImageSection";
import AdminDialog, {
  AdminDialogBody,
  AdminDialogFooter,
  AdminDialogHeader,
} from "../common/AdminDialog";

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
    <AdminDialog open={open} onClose={onClose} size="lg">
        <AdminDialogHeader
          title={(isView && "Chi tiết biến thể") || (isEdit && "Chỉnh sửa biến thể") || "Thêm biến thể"}
          onClose={onClose}
        />

        {/* Body */}
        <AdminDialogBody className="space-y-8">
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

        </AdminDialogBody>

        {/* Footer */}
        {!isView && (
          <AdminDialogFooter>

            <Button
              onClick={onClose}
              className="px-4 py-2 rounded bg-gray-700"
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
