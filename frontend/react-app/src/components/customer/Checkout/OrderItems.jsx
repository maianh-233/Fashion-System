import { Proportions } from "lucide-react";
export default function OrderItems({ items }) {
  return (
    <div className="bg-zinc-900 rounded-2xl p-6">
      <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
        <Proportions size={26} />
        <span>Sản phẩm</span>
      </h2>

      <div className="space-y-4">
        {items.map((item, i) => (
          <div key={i} className="flex gap-4 border-b border-zinc-700 pb-4">
            <img src={item.image} className="w-20 h-20 rounded-xl" />
            <div className="flex-1">
              <p className="font-medium">{item.name}</p>
              <p className="text-sm text-gray-400">
                {item.color} • {item.size}
              </p>
              <p className="text-sm">
                {item.quantity} × {item.price.toLocaleString()} ₫
              </p>
            </div>
            <div className="font-medium">
              {(item.price * item.quantity).toLocaleString()} ₫
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}