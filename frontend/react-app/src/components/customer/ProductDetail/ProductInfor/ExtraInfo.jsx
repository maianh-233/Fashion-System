export default function ExtraInfo() {
  return (
    <div className="grid grid-cols-2 gap-x-6 gap-y-4 mt-8 text-sm">
      <div>
        <strong className="text-gray-300">Chất liệu:</strong>{" "}
        <span className="text-gray-400">100% Cotton</span>
      </div>

      <div>
        <strong className="text-gray-300">Form:</strong>{" "}
        <span className="text-gray-400">Oversize</span>
      </div>

      <div>
        <strong className="text-gray-300">Giới tính:</strong>{" "}
        <span className="text-gray-400">Unisex</span>
      </div>

      <div>
        <strong className="text-gray-300">Xuất xứ:</strong>{" "}
        <span className="text-gray-400">Việt Nam</span>
      </div>
    </div>
  );
}