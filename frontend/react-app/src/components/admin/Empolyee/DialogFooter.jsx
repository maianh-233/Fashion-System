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
    <div className="px-8 py-6 border-t border-zinc-700 bg-zinc-950 flex items-center justify-between">
      
      {/* LEFT: Delete (chỉ khi edit) */}
      <div>
        {isEdit && (
          <button
            onClick={onDelete}
            className="px-6 py-3 rounded-2xl text-red-400 hover:bg-red-950/50 transition"
          >
            Xóa
          </button>
        )}
      </div>

      {/* RIGHT: Action buttons */}
      <div className="flex gap-3">
        {/* Close / Cancel */}
        <button
          onClick={onClose}
          className="px-8 py-3 rounded-2xl text-zinc-300 hover:bg-zinc-800 transition"
        >
          {isView ? "Đóng" : "Hủy"}
        </button>

        {/* View → Edit */}
        {isView && onEdit && (
          <button
            onClick={onEdit}
            className="px-8 py-3 rounded-2xl bg-zinc-700 text-white hover:bg-zinc-600 transition"
          >
            Chỉnh sửa
          </button>
        )}

        {/* Edit / Create → Save */}
        {!isView && (
          <button
            onClick={onSave}
            className="px-8 py-3 rounded-2xl bg-orange-600 hover:bg-orange-500 text-white font-medium transition"
          >
            {isCreate ? "Tạo nhân viên" : "Lưu thay đổi"}
          </button>
        )}
      </div>
    </div>
  );
}