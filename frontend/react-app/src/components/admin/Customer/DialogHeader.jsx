export default function DialogHeader({ mode, onClose }) {
  const titles = {
    view: "Chi Tiết Khách Hàng",
    create: "Thêm Khách Hàng",
    edit: "Chỉnh Sửa Khách Hàng",
  };

  return (
    <div className="px-6 py-5 border-b border-zinc-700 bg-zinc-950 
                    flex justify-between items-center rounded-t-3xl">
      <h2 className="text-2xl font-semibold text-white">
        {titles[mode]}
      </h2>

      <button
        onClick={onClose}
        className="p-2 rounded-xl hover:bg-zinc-800 text-zinc-400 hover:text-white"
      >
        ✕
      </button>
    </div>
  );
}