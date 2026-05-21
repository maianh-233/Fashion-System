export default function BestSellingProducts() {
  const products = [
    {
      name: "Đầm maxi hoa nhí",
      sold: 324,
      revenue: "48.2M đ",
    },
    {
      name: "Áo croptop linen",
      sold: 289,
      revenue: "32.1M đ",
    },
    {
      name: "Quần palazzo đen",
      sold: 231,
      revenue: "28.7M đ",
    },
  ];

  return (
    <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
      <h3 className="font-semibold mb-4">
        Top sản phẩm bán chạy
      </h3>

      <div className="space-y-5">
        {products.map((product, index) => (
          <div
            key={index}
            className="flex items-center gap-4"
          >
            <div className="w-12 h-12 bg-zinc-700 rounded-2xl"></div>

            <div className="flex-1">
              <p className="font-medium">
                {product.name}
              </p>

              <p className="text-sm text-zinc-400">
                {product.sold} đã bán • {product.revenue}
              </p>
            </div>

            <span className="text-emerald-400 font-semibold">
              #{index + 1}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}