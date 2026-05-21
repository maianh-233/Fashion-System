import { TriangleAlert } from "lucide-react";

export default function LowStockList() {
  const items = [
    {
      name: "Váy midi linen be",
      stock: 3,
    },
    {
      name: "Áo sơ mi silk trắng",
      stock: 5,
    },
  ];

  return (
    <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
      <h3 className="font-semibold mb-4 flex items-center gap-2">
        <TriangleAlert className="text-amber-400" size={18} />
        Sản phẩm sắp hết hàng
      </h3>

      <div className="space-y-4">
        {items.map((item, index) => (
          <div
            key={index}
            className="flex justify-between items-center bg-zinc-800/50 p-3 rounded-2xl"
          >
            <div>
              <p className="font-medium">{item.name}</p>

              <p className="text-xs text-zinc-500">
                Còn {item.stock} sản phẩm
              </p>
            </div>

            <span className="text-amber-400 text-sm font-medium">
              Cảnh báo
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}