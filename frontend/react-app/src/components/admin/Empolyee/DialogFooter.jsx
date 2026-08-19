import Button from "../../common/Button";
import { AdminDialogFooter } from "../common/AdminDialog";
export default function DialogFooter({
  mode,          // "view" | "edit" | "create"
  onClose,
  onSave,
  onDelete,
  onEdit,        // optional: dùng khi mode = view
}) {
  const isView = mode === "view";
  const isEdit = mode === "edit";
  const isCreate = mode === "create";

  return (
    <AdminDialogFooter className="justify-between">
      
      {/* LEFT: Delete (chỉ khi edit) */}
      <div>
        {isEdit && (
          <Button
            onClick={onDelete}
            className="px-6 py-3 rounded-2xl text-red-400 hover:bg-red-950/50 transition"
          >
            Xóa
          </Button>
        )}
      </div>

      {/* RIGHT: Action buttons */}
      <div className="flex gap-3">
        {/* Close / Cancel */}
        <Button
          onClick={onClose}
          className="px-8 py-3 rounded-2xl text-zinc-300 hover:bg-zinc-800 transition"
        >
          {isView ? "Đóng" : "Hủy"}
        </Button>

        {/* View → Edit */}
        {isView && onEdit && (
          <Button
            onClick={onEdit}
            className="px-8 py-3 rounded-2xl bg-zinc-700 text-white hover:bg-zinc-600 transition"
          >
            Chỉnh sửa
          </Button>
        )}

        {/* Edit / Create → Save */}
        {!isView && (
          <Button
            onClick={onSave}
            className="px-8 py-3 rounded-2xl bg-orange-600 hover:bg-orange-500 text-white font-medium transition"
          >
            {isCreate ? "Tạo nhân viên" : "Lưu thay đổi"}
          </Button>
        )}
      </div>
    </AdminDialogFooter>
  );
}
