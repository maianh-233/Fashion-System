export default function StoreDialogHeader({ mode, onClose }) {
  return (
    <div className="px-8 py-6 border-b border-zinc-700 flex justify-between">
      <h2 className="text-xl font-semibold">
        {mode === "add" && "Thêm cửa hàng"}
        {mode === "edit" && "Cập nhật cửa hàng"}
        {mode === "view" && "Chi tiết cửa hàng"}
      </h2>
      <button onClick={onClose} className="text-3xl text-zinc-400">×</button>
    </div>
  );
}