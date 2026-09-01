export default function BrandInfo() {
  return (
    <div className="customer-brand-info grid grid-cols-1 md:grid-cols-3 gap-4 mb-12">
      
      {/* Chính hãng */}
      <div className="bg-zinc-900 p-5 rounded-2xl border border-zinc-800">
        <i className="fa-solid fa-check-circle text-emerald-500 text-2xl mb-3"></i>
        <h3 className="font-semibold text-base">Chính hãng 100%</h3>
        <p className="text-gray-400 mt-1">
          Đầy đủ hóa đơn, tem, hộp
        </p>
      </div>

      {/* Giao hàng */}
      <div className="bg-zinc-900 p-5 rounded-2xl border border-zinc-800">
        <i className="fa-solid fa-truck-fast text-blue-500 text-2xl mb-3"></i>
        <h3 className="font-semibold text-base">Giao hàng nhanh</h3>
        <p className="text-gray-400 mt-1">
          Miễn phí từ 500.000đ
        </p>
      </div>

      {/* Bảo hành */}
      <div className="bg-zinc-900 p-5 rounded-2xl border border-zinc-800">
        <i className="fa-solid fa-shield-halved text-amber-500 text-2xl mb-3"></i>
        <h3 className="font-semibold text-base">Bảo hành chính hãng</h3>
        <p className="text-gray-400 mt-1">
          12 – 24 tháng
        </p>
      </div>

    </div>
  );
}
