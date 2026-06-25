// DialogHeader.jsx
export default function DialogHeader({ mode, onClose }) {
  const titles = {
    view: "Chi Tiết Khách Hàng",
    create: "Thêm Khách Hàng",
    edit: "Chỉnh Sửa Khách Hàng",
  };

  return (
    <div className="bg-zinc-950 px-8 py-6 flex justify-between items-center border-b border-[#FFB300]/20">
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