export default function ActionButtons() {
  return (
    <div className="flex flex-col sm:flex-row gap-4 mt-10">
      <button
        onClick={() => alert("✅ Đã thêm vào giỏ hàng!")}
        className="flex-1 bg-[#FFCC00] text-black font-semibold py-4 rounded-3xl text-lg"
      >
        🛒 Thêm vào giỏ hàng
      </button>

      <button className="flex-1 border-2 border-[#FFCC00] text-white py-4 rounded-3xl text-lg hover:bg-[#FFCC00] hover:text-black">
        Mua ngay
      </button>
    </div>
  );
}