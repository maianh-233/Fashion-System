export default function DialogHeader({ mode, onClose }) {
  const titleMap = {
    view: "Chi tiết nhân viên",
    edit: "Chỉnh sửa nhân viên",
    create: "Thêm nhân viên mới",
  };

  return (
    <div className="px-6 py-5 border-b border-zinc-700 bg-zinc-950 flex justify-between items-center">
      <h2 className="text-2xl font-semibold text-white">
        {titleMap[mode]}
      </h2>
      <button
        onClick={onClose}
        className="text-zinc-400 hover:text-white text-3xl"
      >
        ×
      </button>
    </div>
  );
}