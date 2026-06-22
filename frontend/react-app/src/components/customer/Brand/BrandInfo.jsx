export default function BrandInfo() {
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-16">
      
      {/* Chính hãng */}
      <div className="bg-zinc-900 p-8 rounded-3xl border border-zinc-800">
        <i className="fa-solid fa-check-circle text-emerald-500 text-4xl mb-4"></i>
        <h3 className="font-bold text-xl">Chính hãng 100%</h3>
        <p className="text-gray-400 mt-1">
          Đầy đủ hóa đơn, tem, hộp
        </p>
      </div>

      {/* Giao hàng */}
      <div className="bg-zinc-900 p-8 rounded-3xl border border-zinc-800">
        <i className="fa-solid fa-truck-fast text-blue-500 text-4xl mb-4"></i>
        <h3 className="font-bold text-xl">Giao hàng nhanh</h3>
        <p className="text-gray-400 mt-1">
          Miễn phí từ 500.000đ
        </p>
      </div>

      {/* Bảo hành */}
      <div className="bg-zinc-900 p-8 rounded-3xl border border-zinc-800">
        <i className="fa-solid fa-shield-halved text-amber-500 text-4xl mb-4"></i>
        <h3 className="font-bold text-xl">Bảo hành chính hãng</h3>
        <p className="text-gray-400 mt-1">
          12 – 24 tháng
        </p>
      </div>

    </div>
  );
}