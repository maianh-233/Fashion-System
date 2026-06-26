export default function StoreDialogFooter({ mode, onClose, onSubmit }) {
  if (mode === "view") return null;

  return (
    <div className="px-8 py-5 border-t border-zinc-700 flex justify-end gap-3">
      <button onClick={onClose} className="px-5 py-2 bg-zinc-700 rounded-xl">
        Hủy
      </button>
      <button onClick={onSubmit} className="px-5 py-2 bg-blue-600 rounded-xl">
        {mode === "add" ? "Thêm mới" : "Lưu thay đổi"}
      </button>
    </div>
  );
}