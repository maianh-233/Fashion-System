import Button from "../../common/Button";
import PromotionBasicInfo from "./PromotionBasicInfo";
import PromotionDateSection from "./PromotionDateSection";
import PromotionLimitSection from "./PromotionLimitSection";
import PromotionCustomerTier from "./PromotionCustomerTier";
import PromotionMetaInfo from "./PromotionMetaInfo";
import AdminDialog, {
  AdminDialogBody,
  AdminDialogFooter,
  AdminDialogHeader,
} from "../common/AdminDialog";

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
    <AdminDialog open={open} onClose={onClose} size="lg">
        <AdminDialogHeader
          title={(isView && "Chi tiết khuyến mãi") || (isEdit && "Chỉnh sửa khuyến mãi") || "Thêm khuyến mãi"}
          onClose={onClose}
        />

        {/* BODY */}
        <AdminDialogBody className="space-y-8">

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
